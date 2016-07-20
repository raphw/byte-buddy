package net.bytebuddy.dynamic.loading;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
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
public class ClassReloadingStrategy implements ClassLoadingStrategy {

    /**
     * The name of the Byte Buddy {@code net.bytebuddy.agent.Installer} class.
     */
    private static final String INSTALLER_TYPE = "net.bytebuddy.agent.Installer";

    /**
     * The name of the {@code net.bytebuddy.agent.Installer} field containing an installed {@link Instrumentation}.
     */
    private static final String INSTRUMENTATION_FIELD = "instrumentation";

    /**
     * Indicator for accessing a field using reflection to make the code more readable.
     */
    private static final Object STATIC_FIELD = null;

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
        Strategy strategy;
        if (instrumentation.isRedefineClassesSupported()) {
            strategy = Strategy.REDEFINITION;
        } else if (instrumentation.isRetransformClassesSupported()) {
            strategy = Strategy.RETRANSFORMATION;
        } else {
            throw new IllegalArgumentException("Instrumentation does not support manipulation of loaded classes: " + instrumentation);
        }
        return new ClassReloadingStrategy(instrumentation, strategy);
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
     * @return A class reloading strategy which uses the Byte Buddy agent's {@link java.lang.instrument.Instrumentation}.
     */
    public static ClassReloadingStrategy fromInstalledAgent() {
        try {
            Instrumentation instrumentation = (Instrumentation) ClassLoader.getSystemClassLoader()
                    .loadClass(INSTALLER_TYPE)
                    .getDeclaredField(INSTRUMENTATION_FIELD)
                    .get(STATIC_FIELD);
            if (instrumentation == null) {
                throw new IllegalStateException("The Byte Buddy agent is not installed");
            }
            return ClassReloadingStrategy.of(instrumentation);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", exception);
        }
    }

    @Override
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

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        ClassReloadingStrategy that = (ClassReloadingStrategy) other;
        return instrumentation.equals(that.instrumentation)
                && strategy == that.strategy
                && bootstrapInjection.equals(that.bootstrapInjection)
                && preregisteredTypes.equals(that.preregisteredTypes);
    }

    @Override
    public int hashCode() {
        int result = instrumentation.hashCode();
        result = 31 * result + strategy.hashCode();
        result = 31 * result + bootstrapInjection.hashCode();
        result = 31 * result + preregisteredTypes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ClassReloadingStrategy{" +
                "instrumentation=" + instrumentation +
                ", strategy=" + strategy +
                ", bootstrapInjection=" + bootstrapInjection +
                ", preregisteredTypes=" + preregisteredTypes +
                '}';
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
                    instrumentation.addTransformer(classRedefinitionTransformer, REDEFINE_CLASSES);
                    try {
                        instrumentation.retransformClasses(classDefinitions.keySet().toArray(new Class<?>[classDefinitions.size()]));
                    } finally {
                        instrumentation.removeTransformer(classRedefinitionTransformer);
                    }
                }
                classRedefinitionTransformer.assertTransformation();
            }

            @Override
            protected Strategy validate(Instrumentation instrumentation) {
                if (!instrumentation.isRetransformClassesSupported()) {
                    throw new IllegalArgumentException("Does not support retransformation: " + instrumentation);
                }
                return this;
            }

            @Override
            public void reset(Instrumentation instrumentation, ClassFileLocator classFileLocator, List<Class<?>> types) throws IOException, UnmodifiableClassException, ClassNotFoundException {
                instrumentation.addTransformer(ClassResettingTransformer.INSTANCE, true);
                try {
                    instrumentation.retransformClasses(types.toArray(new Class<?>[types.size()]));
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

        @Override
        public String toString() {
            return "ClassReloadingStrategy.Strategy." + name();
        }

        /**
         * Resets the provided types to their original format.
         *
         * @param instrumentation  The instrumentation instance to use for class redefinition or retransformation.
         * @param classFileLocator The class file locator to use.
         * @param types            The types to reset.
         * @throws IOException                If an IO exception occurs.
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

            @Override
            @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Return value is always null")
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

            @Override
            public String toString() {
                return "ClassReloadingStrategy.Strategy.ClassRedefinitionTransformer{redefinedClasses=" + redefinedClasses + '}';
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

            @Override
            public byte[] transform(ClassLoader classLoader,
                                    String internalTypeName,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) {
                return NO_REDEFINITION;
            }

            @Override
            public String toString() {
                return "ClassReloadingStrategy.Strategy.ClassResettingTransformer." + name();
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

            @Override
            public ClassInjector make(Instrumentation instrumentation) {
                throw new IllegalStateException("Bootstrap injection is not enabled");
            }

            @Override
            public String toString() {
                return "ClassReloadingStrategy.BootstrapInjection.Disabled." + name();
            }
        }

        /**
         * An enabled bootstrap class loader injection strategy.
         */
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

            @Override
            public ClassInjector make(Instrumentation instrumentation) {
                return ClassInjector.UsingInstrumentation.of(folder, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && folder.equals(((Enabled) other).folder);
            }

            @Override
            public int hashCode() {
                return folder.hashCode();
            }

            @Override
            public String toString() {
                return "ClassReloadingStrategy.BootstrapInjection.Enabled{" +
                        "folder=" + folder +
                        '}';
            }
        }
    }
}
