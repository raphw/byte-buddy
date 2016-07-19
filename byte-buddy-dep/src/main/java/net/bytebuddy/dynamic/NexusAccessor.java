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
import net.bytebuddy.utility.privilege.SystemClassLoaderAction;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * The Nexus accessor is creating a VM-global singleton {@link Nexus} such that it can be seen by all class loaders of
 * a virtual machine. Furthermore, it provides an API to access this global instance.
 */
public enum NexusAccessor {

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
    NexusAccessor() {
        Dispatcher dispatcher;
        try {
            TypeDescription nexusType = new TypeDescription.ForLoadedType(Nexus.class);
            dispatcher = new Dispatcher.Available(ClassInjector.UsingReflection.ofSystemClassLoader()
                    .inject(Collections.singletonMap(nexusType, ClassFileLocator.ForClassLoader.read(Nexus.class).resolve()))
                    .get(nexusType)
                    .getDeclaredMethod("register", String.class, ClassLoader.class, int.class, Object.class));
        } catch (Exception exception) {
            try {
                dispatcher = new Dispatcher.Available(AccessController.doPrivileged(SystemClassLoaderAction.INSTANCE)
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
                return "NexusAccessor.Dispatcher.Available{" +
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
                return "NexusAccessor.Dispatcher.Unavailable{" +
                        "exception=" + exception +
                        '}';
            }
        }
    }
}
