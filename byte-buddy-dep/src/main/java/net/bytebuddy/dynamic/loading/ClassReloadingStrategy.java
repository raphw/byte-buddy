/*
 * Copyright 2014 - 2018 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.dynamic.loading;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * The class reloading strategy allows to redefine loaded {@link java.lang.Class}es. Note that this strategy
 * will always attempt to load an existing class prior to its redefinition, even if this class is not yet loaded.
 * </p>
 * <p>
 * <b>Note</b>: In order to redefine any type, neither its name or its modifiers must be changed. Furthermore, no
 * fields or methods must be removed or added. This makes this strategy generally incompatible to applying it to a
 * rebased class definition as by {@link net.bytebuddy.ByteBuddy#rebase(Class)} which copies the original method
 * implementations to additional methods. Furthermore, even the {@link net.bytebuddy.ByteBuddy#redefine(Class)}
 * adds a method if the original class contains an explicit <i>class initializer</i>. For these reasons, it is not
 * recommended to use this {@link ClassLoadingStrategy} with arbitrary classes.
 * </p>
 */
@HashCodeAndEqualsPlugin.Enhance
public class ClassReloadingStrategy implements ClassLoadingStrategy<ClassLoader> {

    /**
     * The name of the Byte Buddy {@code net.bytebuddy.agent.Installer} class.
     */
    private static final String INSTALLER_TYPE = "net.bytebuddy.agent.Installer";

    /**
     * The name of the {@code net.bytebuddy.agent.Installer} getter for reading an installed {@link Instrumentation}.
     */
    private static final String INSTRUMENTATION_GETTER = "getInstrumentation";

    /**
     * Indicator for access to a static member via reflection to make the code more readable.
     */
    private static final Object STATIC_MEMBER = null;

    /**
     * A dispatcher to use with some methods of the {@link Instrumentation} API.
     */
    protected static final Dispatcher DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

    /**
     * This instance's instrumentation.
     */
    private final Instrumentation instrumentation;

    /**
     * An strategy which performs the actual redefinition of a {@link java.lang.Class}.
     */
    private final Strategy strategy;

    /**
     * The strategy to apply for injecting classes into the bootstrap class loader.
     */
    private final BootstrapInjection bootstrapInjection;

    /**
     * The preregistered types of this instance.
     */
    private final Map<String, Class<?>> preregisteredTypes;

    /**
     * Creates a class reloading strategy for the given instrumentation using an explicit transformation strategy which
     * is represented by an {@link Strategy}. The given instrumentation
     * must support the strategy's transformation type.
     *
     * @param instrumentation The instrumentation to be used by this reloading strategy.
     * @param strategy        A strategy which performs the actual redefinition of a {@link java.lang.Class}.
     */
    public ClassReloadingStrategy(Instrumentation instrumentation, Strategy strategy) {
        this(instrumentation,
                strategy,
                BootstrapInjection.Disabled.INSTANCE,
                Collections.<String, Class<?>>emptyMap());
    }

    /**
     * Creates a new class reloading strategy.
     *
     * @param instrumentation    The instrumentation to be used by this reloading strategy.
     * @param strategy           An strategy which performs the actual redefinition of a {@link java.lang.Class}.
     * @param bootstrapInjection The bootstrap class loader injection strategy to use.
     * @param preregisteredTypes The preregistered types of this instance.
     */
    protected ClassReloadingStrategy(Instrumentation instrumentation,
                                     Strategy strategy,
                                     BootstrapInjection bootstrapInjection,
                                     Map<String, Class<?>> preregisteredTypes) {
        this.instrumentation = instrumentation;
        this.strategy = strategy.validate(instrumentation);
        this.bootstrapInjection = bootstrapInjection;
        this.preregisteredTypes = preregisteredTypes;
    }

    /**
     * Creates a class reloading strategy for the given instrumentation. The given instrumentation must either
     * support {@link java.lang.instrument.Instrumentation#isRedefineClassesSupported()} or
     * {@link java.lang.instrument.Instrumentation#isRetransformClassesSupported()}. If both modes are supported,
     * classes will be transformed using a class retransformation.
     *
     * @param instrumentation The instrumentation to be used by this reloading strategy.
     * @return A suitable class reloading strategy.
     */
    public static ClassReloadingStrategy of(Instrumentation instrumentation) {
        if (DISPATCHER.isRetransformClassesSupported(instrumentation)) {
            return new ClassReloadingStrategy(instrumentation, Strategy.RETRANSFORMATION);
        } else if (instrumentation.isRedefineClassesSupported()) {
            return new ClassReloadingStrategy(instrumentation, Strategy.REDEFINITION);
        } else {
            throw new IllegalArgumentException("Instrumentation does not support reloading of classes: " + instrumentation);
        }
    }

    /**
     * <p>
     * Obtains a {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy} from an installed Byte Buddy agent. This
     * agent must be installed either by adding the {@code byte-buddy-agent.jar} when starting up the JVM by
     * </p>
     * <p>
     * <code>
     * java -javaagent:byte-buddy-agent.jar -jar app.jar
     * </code>
     * </p>
     * or after the start up using the Attach API. A convenience installer for the OpenJDK is provided by the
     * {@code ByteBuddyAgent} within the {@code byte-buddy-agent} module. The strategy is determined by the agent's support
     * for redefinition where are retransformation is preferred over a redefinition.
     *
     * @return A class reloading strategy which uses the Byte Buddy agent's {@link java.lang.instrument.Instrumentation}.
     */
    public static ClassReloadingStrategy fromInstalledAgent() {
        try {
            return ClassReloadingStrategy.of((Instrumentation) ClassLoader.getSystemClassLoader()
                    .loadClass(INSTALLER_TYPE)
                    .getMethod(INSTRUMENTATION_GETTER)
                    .invoke(STATIC_MEMBER));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", exception);
        }
    }

    /**
     * <p>
     * Obtains a {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy} from an installed Byte Buddy agent. This
     * agent must be installed either by adding the {@code byte-buddy-agent.jar} when starting up the JVM by
     * </p>
     * <p>
     * <code>
     * java -javaagent:byte-buddy-agent.jar -jar app.jar
     * </code>
     * </p>
     * or after the start up using the Attach API. A convenience installer for the OpenJDK is provided by the
     * {@code ByteBuddyAgent} within the {@code byte-buddy-agent} module.
     *
     * @param strategy The strategy to use.
     * @return A class reloading strategy which uses the Byte Buddy agent's {@link java.lang.instrument.Instrumentation}.
     */
    public static ClassReloadingStrategy fromInstalledAgent(Strategy strategy) {
        try {
            return new ClassReloadingStrategy((Instrumentation) ClassLoader.getSystemClassLoader()
                    .loadClass(INSTALLER_TYPE)
                    .getMethod(INSTRUMENTATION_GETTER)
                    .invoke(STATIC_MEMBER), strategy);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
        Map<String, Class<?>> availableTypes = new HashMap<String, Class<?>>(preregisteredTypes);
        for (Class<?> type : instrumentation.getInitiatedClasses(classLoader)) {
            availableTypes.put(TypeDescription.ForLoadedType.getName(type), type);
        }
        Map<Class<?>, ClassDefinition> classDefinitions = new ConcurrentHashMap<Class<?>, ClassDefinition>();
        Map<TypeDescription, Class<?>> loadedClasses = new HashMap<TypeDescription, Class<?>>();
        Map<TypeDescription, byte[]> unloadedClasses = new LinkedHashMap<TypeDescription, byte[]>();
        for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
            Class<?> type = availableTypes.get(entry.getKey().getName());
            if (type != null) {
                classDefinitions.put(type, new ClassDefinition(type, entry.getValue()));
                loadedClasses.put(entry.getKey(), type);
            } else {
                unloadedClasses.put(entry.getKey(), entry.getValue());
            }
        }
        try {
            strategy.apply(instrumentation, classDefinitions);
            if (!unloadedClasses.isEmpty()) {
                loadedClasses.putAll((classLoader == null
                        ? bootstrapInjection.make(instrumentation)
                        : new ClassInjector.UsingReflection(classLoader)).inject(unloadedClasses));
            }
        } catch (ClassNotFoundException exception) {
            throw new IllegalArgumentException("Could not locate classes for redefinition", exception);
        } catch (UnmodifiableClassException exception) {
            throw new IllegalStateException("Cannot redefine specified class", exception);
        }
        return loadedClasses;
    }

    /**
     * Resets all classes to their original definition while using the first type's class loader as a class file locator.
     *
     * @param type The types to reset.
     * @return This class reloading strategy.
     * @throws IOException If a class file locator causes an IO exception.
     */
    public ClassReloadingStrategy reset(Class<?>... type) throws IOException {
        return type.length == 0
                ? this
                : reset(ClassFileLocator.ForClassLoader.of(type[0].getClassLoader()), type);
    }

    /**
     * Resets all classes to their original definition.
     *
     * @param classFileLocator The class file locator to use.
     * @param type             The types to reset.
     * @return This class reloading strategy.
     * @throws IOException If a class file locator causes an IO exception.
     */
    public ClassReloadingStrategy reset(ClassFileLocator classFileLocator, Class<?>... type) throws IOException {
        if (type.length > 0) {
            try {
                strategy.reset(instrumentation, classFileLocator, Arrays.asList(type));
            } catch (ClassNotFoundException exception) {
                throw new IllegalArgumentException("Cannot locate types " + Arrays.toString(type), exception);
            } catch (UnmodifiableClassException exception) {
                throw new IllegalStateException("Cannot reset types " + Arrays.toString(type), exception);
            }
        }
        return this;
    }

    /**
     * Enables bootstrap injection for this class reloading strategy.
     *
     * @param folder The folder to save jar files in that are appended to the bootstrap class path.
     * @return A class reloading strategy with bootstrap injection enabled.
     */
    public ClassReloadingStrategy enableBootstrapInjection(File folder) {
        return new ClassReloadingStrategy(instrumentation, strategy, new BootstrapInjection.Enabled(folder), preregisteredTypes);
    }

    /**
     * Registers a type to be explicitly available without explicit lookup.
     *
     * @param type The loaded types that are explicitly available.
     * @return This class reloading strategy with the given types being explicitly available.
     */
    public ClassReloadingStrategy preregistered(Class<?>... type) {
        Map<String, Class<?>> preregisteredTypes = new HashMap<String, Class<?>>(this.preregisteredTypes);
        for (Class<?> aType : type) {
            preregisteredTypes.put(TypeDescription.ForLoadedType.getName(aType), aType);
        }
        return new ClassReloadingStrategy(instrumentation, strategy, bootstrapInjection, preregisteredTypes);
    }

    /**
     * A dispatcher to interact with the instrumentation API.
     */
    protected interface Dispatcher {

        /**
         * Invokes the {@code Instrumentation#isModifiableClass} method.
         *
         * @param instrumentation The instrumentation instance to invoke the method on.
         * @param type            The type to consider for modifiability.
         * @return {@code true} if the supplied type can be modified.
         */
        boolean isModifiableClass(Instrumentation instrumentation, Class<?> type);

        /**
         * Invokes the {@code Instrumentation#isRetransformClassesSupported} method.
         *
         * @param instrumentation The instrumentation instance to invoke the method on.
         * @return {@code true} if the supplied instrumentation instance supports retransformation.
         */
        boolean isRetransformClassesSupported(Instrumentation instrumentation);

        /**
         * Registers a transformer.
         *
         * @param instrumentation      The instrumentation instance to invoke the method on.
         * @param classFileTransformer The class file transformer to register.
         * @param canRetransform       {@code true} if the class file transformer should be invoked upon a retransformation.
         */
        void addTransformer(Instrumentation instrumentation, ClassFileTransformer classFileTransformer, boolean canRetransform);

        /**
         * Retransforms the supplied classes.
         *
         * @param instrumentation The instrumentation instance to invoke the method on.
         * @param type            The types to retransform.
         * @throws UnmodifiableClassException If any of the supplied types are unmodifiable.
         */
        void retransformClasses(Instrumentation instrumentation, Class<?>[] type) throws UnmodifiableClassException;

        /**
         * An action to create an appropriate {@link Dispatcher}.
         */
        enum CreationAction implements PrivilegedAction<Dispatcher> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public Dispatcher run() {
                try {
                    return new ForJava6CapableVm(Instrumentation.class.getMethod("isModifiableClass", Class.class),
                            Instrumentation.class.getMethod("isRetransformClassesSupported"),
                            Instrumentation.class.getMethod("addTransformer", ClassFileTransformer.class, boolean.class),
                            Instrumentation.class.getMethod("retransformClasses", Class[].class));
                } catch (NoSuchMethodException ignored) {
                    return ForLegacyVm.INSTANCE;
                }
            }
        }

        /**
         * A dispatcher for a legacy VM that does not support retransformation.
         */
        enum ForLegacyVm implements Dispatcher {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public boolean isModifiableClass(Instrumentation instrumentation, Class<?> type) {
                return !type.isArray() && !type.isPrimitive();
            }

            /**
             * {@inheritDoc}
             */
            public boolean isRetransformClassesSupported(Instrumentation instrumentation) {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public void addTransformer(Instrumentation instrumentation, ClassFileTransformer classFileTransformer, boolean canRetransform) {
                if (canRetransform) {
                    throw new UnsupportedOperationException("Cannot apply retransformation on legacy VM");
                } else {
                    instrumentation.addTransformer(classFileTransformer);
                }
            }

            /**
             * {@inheritDoc}
             */
            public void retransformClasses(Instrumentation instrumentation, Class<?>[] type) {
                throw new IllegalStateException();
            }
        }

        /**
         * A dispatcher for a Java 6 capable VM that is potentially capable of retransformation.
         */
        class ForJava6CapableVm implements Dispatcher {

            /**
             * The {@code Instrumentation#isModifiableClass} method.
             */
            private final Method isModifiableClass;

            /**
             * The {@code Instrumentation#isRetransformClassesSupported} method.
             */
            private final Method isRetransformClassesSupported;

            /**
             * The {@code Instrumentation#addTransformer} method.
             */
            private final Method addTransformer;

            /**
             * The {@code Instrumentation#retransformClasses} method.
             */
            private final Method retransformClasses;

            /**
             * Creates a dispatcher for a Java 6 compatible VM.
             *
             * @param isModifiableClass             The {@code Instrumentation#isModifiableClass} method.
             * @param isRetransformClassesSupported The {@code Instrumentation#isRetransformClassesSupported} method.
             * @param addTransformer                The {@code Instrumentation#addTransformer} method.
             * @param retransformClasses            The {@code Instrumentation#retransformClasses} method.
             */
            protected ForJava6CapableVm(Method isModifiableClass,
                                        Method isRetransformClassesSupported,
                                        Method addTransformer,
                                        Method retransformClasses) {
                this.isModifiableClass = isModifiableClass;
                this.isRetransformClassesSupported = isRetransformClassesSupported;
                this.addTransformer = addTransformer;
                this.retransformClasses = retransformClasses;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isModifiableClass(Instrumentation instrumentation, Class<?> type) {
                try {
                    return (Boolean) isModifiableClass.invoke(instrumentation, type);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access java.lang.instrument.Instrumentation#isModifiableClass", exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Error invoking java.lang.instrument.Instrumentation#isModifiableClass", exception.getCause());
                }
            }

            /**
             * {@inheritDoc}
             */
            public boolean isRetransformClassesSupported(Instrumentation instrumentation) {
                try {
                    return (Boolean) isRetransformClassesSupported.invoke(instrumentation);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access java.lang.instrument.Instrumentation#isModifiableClass", exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Error invoking java.lang.instrument.Instrumentation#isModifiableClass", exception.getCause());
                }
            }

            /**
             * {@inheritDoc}
             */
            public void addTransformer(Instrumentation instrumentation, ClassFileTransformer classFileTransformer, boolean canRetransform) {
                try {
                    addTransformer.invoke(instrumentation, classFileTransformer, canRetransform);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access java.lang.instrument.Instrumentation#addTransformer", exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Error invoking java.lang.instrument.Instrumentation#addTransformer", exception.getCause());
                }
            }

            /**
             * {@inheritDoc}
             */
            public void retransformClasses(Instrumentation instrumentation, Class<?>[] type) throws UnmodifiableClassException {
                try {
                    retransformClasses.invoke(instrumentation, (Object) type);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access java.lang.instrument.Instrumentation#retransformClasses", exception);
                } catch (InvocationTargetException exception) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof UnmodifiableClassException) {
                        throw (UnmodifiableClassException) cause;
                    } else {
                        throw new IllegalStateException("Error invoking java.lang.instrument.Instrumentation#retransformClasses", cause);
                    }
                }
            }
        }
    }

    /**
     * A strategy which performs the actual redefinition of a {@link java.lang.Class}.
     */
    public enum Strategy {

        /**
         * <p>
         * Redefines a class using {@link java.lang.instrument.Instrumentation#redefineClasses(java.lang.instrument.ClassDefinition...)}.
         * </p>
         * <p>
         * This strategy can be more efficient. However, the redefinition strategy does not allow to reset VM anonymous classes (e.g.
         * classes that represent lambda expressions).
         * </p>
         */
        REDEFINITION(true) {
            @Override
            protected void apply(Instrumentation instrumentation, Map<Class<?>, ClassDefinition> classDefinitions)
                    throws UnmodifiableClassException, ClassNotFoundException {
                instrumentation.redefineClasses(classDefinitions.values().toArray(new ClassDefinition[classDefinitions.size()]));
            }

            @Override
            protected Strategy validate(Instrumentation instrumentation) {
                if (!instrumentation.isRedefineClassesSupported()) {
                    throw new IllegalArgumentException("Does not support redefinition: " + instrumentation);
                }
                return this;
            }

            @Override
            public void reset(Instrumentation instrumentation, ClassFileLocator classFileLocator, List<Class<?>> types)
                    throws IOException, UnmodifiableClassException, ClassNotFoundException {
                Map<Class<?>, ClassDefinition> classDefinitions = new HashMap<Class<?>, ClassDefinition>(types.size());
                for (Class<?> type : types) {
                    classDefinitions.put(type, new ClassDefinition(type, classFileLocator.locate(TypeDescription.ForLoadedType.getName(type)).resolve()));
                }
                apply(instrumentation, classDefinitions);
            }
        },

        /**
         * <p>
         * Redefines a class using
         * {@link java.lang.instrument.Instrumentation#retransformClasses(Class[])}. This requires synchronization on
         * the {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy#instrumentation} object.
         * </p>
         * <p>
         * This strategy can require more time to be applied but does not struggle to reset VM anonymous classes (e.g. classes
         * that represent lambda expressions).
         * </p>
         */
        RETRANSFORMATION(false) {
            @Override
            protected void apply(Instrumentation instrumentation, Map<Class<?>, ClassDefinition> classDefinitions) throws UnmodifiableClassException {
                ClassRedefinitionTransformer classRedefinitionTransformer = new ClassRedefinitionTransformer(classDefinitions);
                synchronized (this) {
                    DISPATCHER.addTransformer(instrumentation, classRedefinitionTransformer, REDEFINE_CLASSES);
                    try {
                        DISPATCHER.retransformClasses(instrumentation, classDefinitions.keySet().toArray(new Class<?>[classDefinitions.size()]));
                    } finally {
                        instrumentation.removeTransformer(classRedefinitionTransformer);
                    }
                }
                classRedefinitionTransformer.assertTransformation();
            }

            @Override
            protected Strategy validate(Instrumentation instrumentation) {
                if (!DISPATCHER.isRetransformClassesSupported(instrumentation)) {
                    throw new IllegalArgumentException("Does not support retransformation: " + instrumentation);
                }
                return this;
            }

            @Override
            public void reset(Instrumentation instrumentation, ClassFileLocator classFileLocator, List<Class<?>> types) throws UnmodifiableClassException, ClassNotFoundException {
                for (Class<?> type : types) {
                    if (!DISPATCHER.isModifiableClass(instrumentation, type)) {
                        throw new IllegalArgumentException("Cannot modify type: " + type);
                    }
                }
                DISPATCHER.addTransformer(instrumentation, ClassResettingTransformer.INSTANCE, REDEFINE_CLASSES);
                try {
                    DISPATCHER.retransformClasses(instrumentation, types.toArray(new Class<?>[types.size()]));
                } finally {
                    instrumentation.removeTransformer(ClassResettingTransformer.INSTANCE);
                }
            }
        };

        /**
         * Indicates that a class is not redefined.
         */
        private static final byte[] NO_REDEFINITION = null;

        /**
         * Instructs a {@link java.lang.instrument.ClassFileTransformer} to redefine classes.
         */
        private static final boolean REDEFINE_CLASSES = true;

        /**
         * {@code true} if the {@link Strategy#REDEFINITION} strategy
         * is used.
         */
        private final boolean redefinition;

        /**
         * Creates a new strategy.
         *
         * @param redefinition {@code true} if the {@link Strategy#REDEFINITION} strategy is used.
         */
        Strategy(boolean redefinition) {
            this.redefinition = redefinition;
        }

        /**
         * Applies this strategy for the given arguments.
         *
         * @param instrumentation  The instrumentation to be used for applying the redefinition.
         * @param classDefinitions A mapping of the classes to be redefined to their redefinition.
         * @throws UnmodifiableClassException If a class is not modifiable.
         * @throws ClassNotFoundException     If a class was not found.
         */
        protected abstract void apply(Instrumentation instrumentation, Map<Class<?>, ClassDefinition> classDefinitions)
                throws UnmodifiableClassException, ClassNotFoundException;

        /**
         * Validates that this strategy supports a given transformation type.
         *
         * @param instrumentation The instrumentation instance being used.
         * @return This strategy.
         */
        protected abstract Strategy validate(Instrumentation instrumentation);

        /**
         * Returns {@code true} if this strategy represents {@link Strategy#REDEFINITION}.
         *
         * @return {@code true} if this strategy represents {@link Strategy#REDEFINITION}.
         */
        public boolean isRedefinition() {
            return redefinition;
        }

        /**
         * Resets the provided types to their original format.
         *
         * @param instrumentation  The instrumentation instance to use for class redefinition or retransformation.
         * @param classFileLocator The class file locator to use.
         * @param types            The types to reset.
         * @throws IOException                If an I/O exception occurs.
         * @throws UnmodifiableClassException If a class is not modifiable.
         * @throws ClassNotFoundException     If a class could not be found.
         */
        public abstract void reset(Instrumentation instrumentation, ClassFileLocator classFileLocator, List<Class<?>> types) throws IOException, UnmodifiableClassException, ClassNotFoundException;

        /**
         * A class file transformer that applies a given {@link java.lang.instrument.ClassDefinition}.
         */
        protected static class ClassRedefinitionTransformer implements ClassFileTransformer {

            /**
             * A mapping of classes to be redefined to their redefined class definitions.
             */
            private final Map<Class<?>, ClassDefinition> redefinedClasses;

            /**
             * Creates a new class redefinition transformer.
             *
             * @param redefinedClasses A mapping of classes to be redefined to their redefined class definitions.
             */
            protected ClassRedefinitionTransformer(Map<Class<?>, ClassDefinition> redefinedClasses) {
                this.redefinedClasses = redefinedClasses;
            }

            /**
             * {@inheritDoc}
             */
            @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Value is always null")
            public byte[] transform(ClassLoader classLoader,
                                    String internalTypeName,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) {
                if (internalTypeName == null) {
                    return NO_REDEFINITION;
                }
                ClassDefinition redefinedClass = redefinedClasses.remove(classBeingRedefined);
                return redefinedClass == null
                        ? NO_REDEFINITION
                        : redefinedClass.getDefinitionClassFile();
            }

            /**
             * Validates that all given classes were redefined.
             */
            public void assertTransformation() {
                if (!redefinedClasses.isEmpty()) {
                    throw new IllegalStateException("Could not transform: " + redefinedClasses.keySet());
                }
            }
        }

        /**
         * A transformer that indicates that a class file should not be transformed.
         */
        protected enum ClassResettingTransformer implements ClassFileTransformer {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public byte[] transform(ClassLoader classLoader,
                                    String internalTypeName,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) {
                return NO_REDEFINITION;
            }
        }
    }

    /**
     * A strategy to apply for injecting classes into the bootstrap class loader.
     */
    protected interface BootstrapInjection {

        /**
         * Creates a class injector to use.
         *
         * @param instrumentation The instrumentation of this instance.
         * @return A class injector for the bootstrap class loader.
         */
        ClassInjector make(Instrumentation instrumentation);

        /**
         * A disabled bootstrap injection strategy.
         */
        enum Disabled implements BootstrapInjection {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public ClassInjector make(Instrumentation instrumentation) {
                throw new IllegalStateException("Bootstrap injection is not enabled");
            }
        }

        /**
         * An enabled bootstrap class loader injection strategy.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Enabled implements BootstrapInjection {

            /**
             * The folder to save jar files in.
             */
            private final File folder;

            /**
             * Creates an enabled bootstrap class injection strategy.
             *
             * @param folder The folder to save jar files in.
             */
            protected Enabled(File folder) {
                this.folder = folder;
            }

            /**
             * {@inheritDoc}
             */
            public ClassInjector make(Instrumentation instrumentation) {
                return ClassInjector.UsingInstrumentation.of(folder, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation);
            }
        }
    }
}
