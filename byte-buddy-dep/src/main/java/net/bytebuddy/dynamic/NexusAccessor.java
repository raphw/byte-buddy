package net.bytebuddy.dynamic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassInjector;
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * The Nexus accessor is creating a VM-global singleton {@link Nexus} such that it can be seen by all class loaders of
 * a virtual machine. Furthermore, it provides an API to access this global instance.
 */
public enum NexusAccessor implements PrivilegedAction<NexusAccessor.Dispatcher> {

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
    NexusAccessor() {
        dispatcher = AccessController.doPrivileged(this);
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
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
    public Dispatcher run() {
        try {
            Class<?> nexusType = new ClassInjector.UsingReflection(ClassLoader.getSystemClassLoader(), Nexus.class.getProtectionDomain())
                    .inject(Collections.singletonMap(new TypeDescription.ForLoadedType(Nexus.class), ClassFileLocator.ForClassLoader.read(Nexus.class).resolve()))
                    .get(new TypeDescription.ForLoadedType(Nexus.class));
            return new Dispatcher.Available(nexusType.getDeclaredMethod("register", String.class, ClassLoader.class, int.class, Object.class),
                    nexusType.getDeclaredMethod("clean"));
        } catch (Exception exception) {
            try {
                Class<?> nexusType = ClassLoader.getSystemClassLoader().loadClass(Nexus.class.getName());
                return new Dispatcher.Available(nexusType.getDeclaredMethod("register", String.class, ClassLoader.class, int.class, Object.class),
                        nexusType.getDeclaredMethod("clean"));
            } catch (Exception ignored) {
                return new Dispatcher.Unavailable(exception);
            }
        }
    }

    /**
     * Checks if this {@link NexusAccessor} is capable of registering loaded type initializers.
     *
     * @return {@code true} if this accessor is alive.
     */
    public boolean isAlive() {
        return dispatcher.isAlive();
    }

    /**
     * Removes any stale entries that are registered in the {@link Nexus}. Entries can become stale if a class is loaded but never initialized
     * prior to its garbage collection. As all class loaders within a nexus are only referenced weakly, such class loaders are always garbage
     * collected. However, the initialization data stored by Byte Buddy does not become eligible which is why it needs to be cleaned explicitly.
     */
    public void clean() {
        dispatcher.clean();
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
        return "NexusAccessor." + name();
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
            return "NexusAccessor.InitializationAppender{" +
                    "identification=" + identification +
                    '}';
        }
    }

    /**
     * A dispatcher for registering type initializers in the {@link Nexus}.
     */
    protected interface Dispatcher {

        /**
         * Returns {@code true} if this dispatcher is alive.
         *
         * @return {@code true} if this dispatcher is alive.
         */
        boolean isAlive();

        /**
         * Cleans any dead entries of the system class loader's {@link Nexus}.
         */
        void clean();

        /**
         * Registers a type initializer with the system class loader's nexus.
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
             * The {@link Nexus#register(String, ClassLoader, int, Object)} method.
             */
            private final Method register;

            /**
             * The {@link Nexus#clean()} method.
             */
            private final Method clean;

            /**
             * Creates a new dispatcher.
             *
             * @param register The {@link Nexus#register(String, ClassLoader, int, Object)} method.
             * @param clean    The {@link Nexus#clean()} method.
             */
            protected Available(Method register, Method clean) {
                this.register = register;
                this.clean = clean;
            }

            @Override
            public boolean isAlive() {
                return true;
            }

            @Override
            public void clean() {
                try {
                    clean.invoke(STATIC_METHOD);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access: " + clean, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke: " + clean, exception.getCause());
                }
            }

            @Override
            public void register(String name, ClassLoader classLoader, int identification, LoadedTypeInitializer loadedTypeInitializer) {
                try {
                    register.invoke(STATIC_METHOD, name, classLoader, identification, loadedTypeInitializer);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access: " + register, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke: " + register, exception.getCause());
                }
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && register.equals(((Available) other).register)
                        && clean.equals(((Available) other).clean);
            }

            @Override
            public int hashCode() {
                return register.hashCode() + 31 * clean.hashCode();
            }

            @Override
            public String toString() {
                return "NexusAccessor.Dispatcher.Available{" +
                        "register=" + register +
                        ", clean=" + clean +
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
            public boolean isAlive() {
                return false;
            }

            @Override
            public void clean() {
                /* do nothing */
            }

            @Override
            public void register(String name, ClassLoader classLoader, int identification, LoadedTypeInitializer loadedTypeInitializer) {
                throw new IllegalStateException("Could not locate Nexus method", exception);
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
                return "NexusAccessor.Dispatcher.Unavailable{" +
                        "exception=" + exception +
                        '}';
            }
        }
    }
}
