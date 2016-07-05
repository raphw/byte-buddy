package net.bytebuddy.dynamic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import net.bytebuddy.implementation.bytecode.constant.ClassConstant;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * A type resolver is responsible for loading a class and for initializing its {@link LoadedTypeInitializer}s.
 */
public interface TypeResolver {

    /**
     * Resolves a type resolver for actual application.
     *
     * @return A resolved version of this type resolver.
     */
    Resolved resolve();

    /**
     * A resolved {@link TypeResolver}.
     */
    interface Resolved {

        /**
         * Injects a type initializer into the supplied type initializer, if applicable. This way, a type resolver
         * is capable of injecting code into the generated class's initializer to inline the initialization.
         *
         * @param typeInitializer The type initializer to potentially expend.
         * @return A type initializer to apply for performing the represented type resolution.
         */
        TypeInitializer injectedInto(TypeInitializer typeInitializer);

        /**
         * Loads and initializes a dynamic type.
         *
         * @param dynamicType          The dynamic type to initialize.
         * @param classLoader          The class loader to use.
         * @param classLoadingStrategy The class loading strategy to use.
         * @return A map of all type descriptions mapped to their representation as a loaded class.
         */
        Map<TypeDescription, Class<?>> initialize(DynamicType dynamicType, ClassLoader classLoader, ClassLoadingStrategy classLoadingStrategy);
    }

    /**
     * A type resolver that applies all {@link LoadedTypeInitializer} after class loading using reflection. This implies that the initializers
     * are executed <b>after</b> a type initializer is executed.
     */
    enum Passive implements TypeResolver, Resolved {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Resolved resolve() {
            return this;
        }

        @Override
        public TypeInitializer injectedInto(TypeInitializer typeInitializer) {
            return typeInitializer;
        }

        @Override
        public Map<TypeDescription, Class<?>> initialize(DynamicType dynamicType, ClassLoader classLoader, ClassLoadingStrategy classLoadingStrategy) {
            Map<TypeDescription, Class<?>> types = classLoadingStrategy.load(classLoader, dynamicType.getAllTypes());
            for (Map.Entry<TypeDescription, LoadedTypeInitializer> entry : dynamicType.getLoadedTypeInitializers().entrySet()) {
                entry.getValue().onLoad(types.get(entry.getKey()));
            }
            return new HashMap<TypeDescription, Class<?>>(types);
        }

        @Override
        public String toString() {
            return "TypeResolver.Passive." + name();
        }
    }

    /**
     * A type resolver that applies all {@link LoadedTypeInitializer} as a part of class loading using reflection. This implies that the initializers
     * are executed <b>before</b> (as a first action of) a type initializer is executed.
     */
    enum Active implements TypeResolver {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * The dispatcher to use.
         */
        private final Dispatcher dispatcher;

        /**
         * The {@link ClassLoader#getSystemClassLoader()} method.
         */
        private final MethodDescription.InDefinedShape getSystemClassLoader;

        /**
         * The {@link java.lang.ClassLoader#loadClass(String)} method.
         */
        private final MethodDescription.InDefinedShape loadClass;

        /**
         * The {@link Integer#valueOf(int)} method.
         */
        private final MethodDescription.InDefinedShape valueOf;

        /**
         * The {@link java.lang.Class#getDeclaredMethod(String, Class[])} method.
         */
        private final MethodDescription getDeclaredMethod;

        /**
         * The {@link java.lang.reflect.Method#invoke(Object, Object...)} method.
         */
        private final MethodDescription invokeMethod;

        /**
         * Creates the singleton accessor.
         */
        @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Explicit delegation of the exception")
        Active() {
            Dispatcher dispatcher;
            try {
                TypeDescription nexusType = new TypeDescription.ForLoadedType(Nexus.class);
                dispatcher = new Dispatcher.Available(ClassInjector.UsingReflection.ofSystemClassLoader()
                        .inject(Collections.singletonMap(nexusType, ClassFileLocator.ForClassLoader.read(Nexus.class).resolve()))
                        .get(nexusType)
                        .getDeclaredMethod("register", String.class, ClassLoader.class, int.class, Object.class));
            } catch (Exception exception) {
                try {
                    dispatcher = new Dispatcher.Available(ClassLoader.getSystemClassLoader()
                            .loadClass(Nexus.class.getName())
                            .getDeclaredMethod("register", String.class, ClassLoader.class, int.class, Object.class));
                } catch (Exception ignored) {
                    dispatcher = new Dispatcher.Unavailable(exception);
                }
            }
            this.dispatcher = dispatcher;
            getSystemClassLoader = new TypeDescription.ForLoadedType(ClassLoader.class).getDeclaredMethods()
                    .filter(named("getSystemClassLoader").and(takesArguments(0))).getOnly();
            loadClass = new TypeDescription.ForLoadedType(ClassLoader.class).getDeclaredMethods()
                    .filter(named("loadClass").and(takesArguments(String.class))).getOnly();
            getDeclaredMethod = new TypeDescription.ForLoadedType(Class.class).getDeclaredMethods()
                    .filter(named("getDeclaredMethod").and(takesArguments(String.class, Class[].class))).getOnly();
            invokeMethod = new TypeDescription.ForLoadedType(Method.class).getDeclaredMethods()
                    .filter(named("invoke").and(takesArguments(Object.class, Object[].class))).getOnly();
            valueOf = new TypeDescription.ForLoadedType(Integer.class).getDeclaredMethods()
                    .filter(named("valueOf").and(takesArguments(int.class))).getOnly();
        }

        @Override
        public TypeResolver.Resolved resolve() {
            return new Resolved(new Random().nextInt());
        }

        /**
         * Registers a loaded type initializer in Byte Buddy's {@link Nexus} which is injected into the system class loader.
         *
         * @param name                  The binary name of the class.
         * @param classLoader           The class's class loader.
         * @param identification        The id used for identifying the loaded type initializer that was added to the {@link Nexus}.
         * @param loadedTypeInitializer The loaded type initializer to make available via the {@link Nexus}.
         */
        public void register(String name, ClassLoader classLoader, int identification, LoadedTypeInitializer loadedTypeInitializer) {
            if (loadedTypeInitializer.isAlive()) {
                dispatcher.register(name, classLoader, identification, loadedTypeInitializer);
            }
        }

        @Override
        public String toString() {
            return "TypeResolver.Active." + name();
        }

        /**
         * A dispatcher for registering type initializers in the {@link Nexus}.
         */
        protected interface Dispatcher {

            /**
             * Registers a type initializer with the class loader's nexus.
             *
             * @param name                  The name of a type for which a loaded type initializer is registered.
             * @param classLoader           The class loader for which a loaded type initializer is registered.
             * @param identification        An identification for the initializer to run.
             * @param loadedTypeInitializer The loaded type initializer to be registered.
             */
            void register(String name, ClassLoader classLoader, int identification, LoadedTypeInitializer loadedTypeInitializer);

            /**
             * An enabled dispatcher for registering a type initializer in a {@link Nexus}.
             */
            class Available implements Dispatcher {

                /**
                 * Indicates that a static method is invoked by reflection.
                 */
                private static final Object STATIC_METHOD = null;

                /**
                 * The method for registering a type initializer in the system class loader's {@link Nexus}.
                 */
                private final Method registration;

                /**
                 * Creates a new dispatcher.
                 *
                 * @param registration The method for registering a type initializer in the system class loader's {@link Nexus}.
                 */
                protected Available(Method registration) {
                    this.registration = registration;
                }

                @Override
                public void register(String name, ClassLoader classLoader, int identification, LoadedTypeInitializer loadedTypeInitializer) {
                    try {
                        registration.invoke(STATIC_METHOD, name, classLoader, identification, loadedTypeInitializer);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot register type initializer for " + name, exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Cannot register type initializer for " + name, exception.getCause());
                    }
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && registration.equals(((Available) other).registration);
                }

                @Override
                public int hashCode() {
                    return registration.hashCode();
                }

                @Override
                public String toString() {
                    return "TypeResolver.Active.Dispatcher.Available{" +
                            "registration=" + registration +
                            '}';
                }
            }

            /**
             * A disabled dispatcher where a {@link Nexus} is not available.
             */
            class Unavailable implements Dispatcher {

                /**
                 * The exception that was raised during the dispatcher initialization.
                 */
                private final Exception exception;

                /**
                 * Creates a new disabled dispatcher.
                 *
                 * @param exception The exception that was raised during the dispatcher initialization.
                 */
                protected Unavailable(Exception exception) {
                    this.exception = exception;
                }

                @Override
                public void register(String name, ClassLoader classLoader, int identification, LoadedTypeInitializer loadedTypeInitializer) {
                    throw new IllegalStateException("Could not locate registration method", exception);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && exception.equals(((Unavailable) other).exception);
                }

                @Override
                public int hashCode() {
                    return exception.hashCode();
                }

                @Override
                public String toString() {
                    return "TypeResolver.Active.Dispatcher.Unavailable{" +
                            "exception=" + exception +
                            '}';
                }
            }
        }

        /**
         * A resolved version of an active type resolver.
         */
        protected static class Resolved implements TypeResolver.Resolved {

            /**
             * The id used for identifying the loaded type initializer that was added to the {@link Nexus}.
             */
            private final int identification;

            /**
             * Creates a new resolved active type resolver.
             *
             * @param identification The id used for identifying the loaded type initializer that was added to the {@link Nexus}.
             */
            protected Resolved(int identification) {
                this.identification = identification;
            }

            @Override
            public TypeInitializer injectedInto(TypeInitializer typeInitializer) {
                return typeInitializer.expandWith(new InitializationAppender(identification));
            }

            @Override
            public Map<TypeDescription, Class<?>> initialize(DynamicType dynamicType, ClassLoader classLoader, ClassLoadingStrategy classLoadingStrategy) {
                Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = new HashMap<TypeDescription, LoadedTypeInitializer>(dynamicType.getLoadedTypeInitializers());
                TypeDescription instrumentedType = dynamicType.getTypeDescription();
                Map<TypeDescription, Class<?>> types = classLoadingStrategy.load(classLoader, dynamicType.getAllTypes());
                INSTANCE.register(instrumentedType.getName(),
                        types.get(instrumentedType).getClassLoader(),
                        identification,
                        loadedTypeInitializers.remove(instrumentedType));
                for (Map.Entry<TypeDescription, LoadedTypeInitializer> entry : loadedTypeInitializers.entrySet()) {
                    entry.getValue().onLoad(types.get(entry.getKey()));
                }
                return types;
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                Resolved resolved = (Resolved) object;
                return identification == resolved.identification;
            }

            @Override
            public int hashCode() {
                return identification;
            }

            @Override
            public String toString() {
                return "TypeResolver.Active.Resolved{" +
                        "identification=" + identification +
                        '}';
            }
        }

        /**
         * An initialization appender that looks up a loaded type initializer from Byte Buddy's {@link Nexus}.
         */
        public static class InitializationAppender implements ByteCodeAppender {

            /**
             * The id used for identifying the loaded type initializer that was added to the {@link Nexus}.
             */
            private final int identification;

            /**
             * Creates a new initialization appender.
             *
             * @param identification The id used for identifying the loaded type initializer that was added to the {@link Nexus}.
             */
            public InitializationAppender(int identification) {
                this.identification = identification;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                return new ByteCodeAppender.Simple(new StackManipulation.Compound(
                        MethodInvocation.invoke(INSTANCE.getSystemClassLoader),
                        new TextConstant(Nexus.class.getName()),
                        MethodInvocation.invoke(INSTANCE.loadClass),
                        new TextConstant("initialize"),
                        ArrayFactory.forType(new TypeDescription.Generic.OfNonGenericType.ForLoadedType(Class.class))
                                .withValues(Arrays.asList(
                                        ClassConstant.of(TypeDescription.CLASS),
                                        ClassConstant.of(new TypeDescription.ForLoadedType(int.class)))),
                        MethodInvocation.invoke(INSTANCE.getDeclaredMethod),
                        NullConstant.INSTANCE,
                        ArrayFactory.forType(TypeDescription.Generic.OBJECT)
                                .withValues(Arrays.asList(
                                        ClassConstant.of(instrumentedMethod.getDeclaringType().asErasure()),
                                        new StackManipulation.Compound(
                                                IntegerConstant.forValue(identification),
                                                MethodInvocation.invoke(INSTANCE.valueOf)))),
                        MethodInvocation.invoke(INSTANCE.invokeMethod),
                        Removal.SINGLE
                )).apply(methodVisitor, implementationContext, instrumentedMethod);
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                InitializationAppender that = (InitializationAppender) object;
                return identification == that.identification;
            }

            @Override
            public int hashCode() {
                return identification;
            }

            @Override
            public String toString() {
                return "TypeResolver.Active.InitializationAppender{" +
                        "identification=" + identification +
                        '}';
            }
        }
    }

    /**
     * A type resolver that does not apply any {@link LoadedTypeInitializer}s but only loads all types.
     */
    enum Lazy implements TypeResolver, Resolved {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Resolved resolve() {
            return this;
        }

        @Override
        public TypeInitializer injectedInto(TypeInitializer typeInitializer) {
            return typeInitializer;
        }

        @Override
        public Map<TypeDescription, Class<?>> initialize(DynamicType dynamicType, ClassLoader classLoader, ClassLoadingStrategy classLoadingStrategy) {
            return classLoadingStrategy.load(classLoader, dynamicType.getAllTypes());
        }

        @Override
        public String toString() {
            return "TypeResolver.Lazy." + name();
        }
    }

    /**
     * A type resolver that does not allow for explicit loading of a class and that does not inject any code into the type initializer.
     */
    enum Disabled implements TypeResolver, Resolved {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Resolved resolve() {
            return this;
        }

        @Override
        public TypeInitializer injectedInto(TypeInitializer typeInitializer) {
            return typeInitializer;
        }

        @Override
        public Map<TypeDescription, Class<?>> initialize(DynamicType dynamicType, ClassLoader classLoader, ClassLoadingStrategy classLoadingStrategy) {
            throw new IllegalStateException("Cannot initialize a dynamic type for a disabled type resolver");
        }

        @Override
        public String toString() {
            return "TypeResolver.Disabled." + name();
        }
    }
}
