package net.bytebuddy.dynamic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;

/**
 * The Nexus accessor is creating a VM-global singleton {@link Nexus} such that it can be seen by all class loaders of
 * a virtual machine. Furthermore, it provides an API to access this global instance.
 */
@HashCodeAndEqualsPlugin.Enhance
public class NexusAccessor {

    /**
     * The dispatcher to use.
     */
    private static final Dispatcher DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

    /**
     * The reference queue that is notified upon a GC eligible {@link Nexus} entry or {@code null} if no such queue should be notified.
     */
    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
    private final ReferenceQueue<? super ClassLoader> referenceQueue;

    /**
     * Creates a new accessor for the {@link Nexus} without any active management of stale references within a nexus.
     */
    public NexusAccessor() {
        this(Nexus.NO_QUEUE);
    }

    /**
     * Creates a new accessor for a {@link Nexus} where any GC eligible are enqueued to the supplied reference queue. Any such enqueued
     * reference can be explicitly removed from the nexus via the {@link NexusAccessor#clean(Reference)} method. Nexus entries can
     * become stale if a class loader is garbage collected after a class was loaded but before a class was initialized.
     *
     * @param referenceQueue The reference queue onto which stale references should be enqueued or {@code null} if no reference queue
     *                       should be notified.
     */
    public NexusAccessor(ReferenceQueue<? super ClassLoader> referenceQueue) {
        this.referenceQueue = referenceQueue;
    }

    /**
     * Checks if this {@link NexusAccessor} is capable of registering loaded type initializers.
     *
     * @return {@code true} if this accessor is alive.
     */
    public static boolean isAlive() {
        return DISPATCHER.isAlive();
    }

    /**
     * Removes a stale entries that are registered in the {@link Nexus}. Entries can become stale if a class is loaded but never initialized
     * prior to its garbage collection. As all class loaders within a nexus are only referenced weakly, such class loaders are always garbage
     * collected. However, the initialization data stored by Byte Buddy does not become eligible which is why it needs to be cleaned explicitly.
     *
     * @param reference The reference to remove. References are collected via a reference queue that is supplied to the {@link NexusAccessor}.
     */
    public static void clean(Reference<? extends ClassLoader> reference) {
        DISPATCHER.clean(reference);
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
            DISPATCHER.register(name, classLoader, referenceQueue, identification, loadedTypeInitializer);
        }
    }

    /**
     * An initialization appender that looks up a loaded type initializer from Byte Buddy's {@link Nexus}.
     */
    @HashCodeAndEqualsPlugin.Enhance
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
            try {
                return new ByteCodeAppender.Simple(new StackManipulation.Compound(
                        MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(ClassLoader.class.getMethod("getSystemClassLoader"))),
                        new TextConstant(Nexus.class.getName()),
                        MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(ClassLoader.class.getMethod("loadClass", String.class))),
                        new TextConstant("initialize"),
                        ArrayFactory.forType(new TypeDescription.Generic.OfNonGenericType.ForLoadedType(Class.class))
                                .withValues(Arrays.asList(
                                        ClassConstant.of(TypeDescription.CLASS),
                                        ClassConstant.of(new TypeDescription.ForLoadedType(int.class)))),
                        MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(Class.class.getMethod("getMethod", String.class, Class[].class))),
                        NullConstant.INSTANCE,
                        ArrayFactory.forType(TypeDescription.Generic.OBJECT)
                                .withValues(Arrays.asList(
                                        ClassConstant.of(instrumentedMethod.getDeclaringType().asErasure()),
                                        new StackManipulation.Compound(
                                                IntegerConstant.forValue(identification),
                                                MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(Integer.class.getMethod("valueOf", int.class)))))),
                        MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(Method.class.getMethod("invoke", Object.class, Object[].class))),
                        Removal.SINGLE
                )).apply(methodVisitor, implementationContext, instrumentedMethod);
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException("Cannot locate method", exception);
            }
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
         *
         * @param reference The reference to remove.
         */
        void clean(Reference<? extends ClassLoader> reference);

        /**
         * Registers a type initializer with the system class loader's nexus.
         *
         * @param name                  The name of a type for which a loaded type initializer is registered.
         * @param classLoader           The class loader for which a loaded type initializer is registered.
         * @param referenceQueue        A reference queue to notify about stale nexus entries or {@code null} if no queue should be referenced.
         * @param identification        An identification for the initializer to run.
         * @param loadedTypeInitializer The loaded type initializer to be registered.
         */
        void register(String name,
                      ClassLoader classLoader,
                      ReferenceQueue<? super ClassLoader> referenceQueue,
                      int identification,
                      LoadedTypeInitializer loadedTypeInitializer);

        /**
         * Creates a new dispatcher for accessing a {@link Nexus}.
         */
        enum CreationAction implements PrivilegedAction<Dispatcher> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
            public Dispatcher run() {
                if (Boolean.getBoolean(Nexus.PROPERTY)) {
                    return new Unavailable(new IllegalStateException("Nexus injection was explicitly disabled"));
                } else {
                    try {
                        Class<?> nexusType = new ClassInjector.UsingReflection(ClassLoader.getSystemClassLoader(), ClassLoadingStrategy.NO_PROTECTION_DOMAIN)
                                .inject(Collections.singletonMap(new TypeDescription.ForLoadedType(Nexus.class), ClassFileLocator.ForClassLoader.read(Nexus.class).resolve()))
                                .get(new TypeDescription.ForLoadedType(Nexus.class));
                        return new Dispatcher.Available(nexusType.getMethod("register", String.class, ClassLoader.class, ReferenceQueue.class, int.class, Object.class),
                                nexusType.getMethod("clean", Reference.class));
                    } catch (Exception exception) {
                        try {
                            Class<?> nexusType = ClassLoader.getSystemClassLoader().loadClass(Nexus.class.getName());
                            return new Dispatcher.Available(nexusType.getMethod("register", String.class, ClassLoader.class, ReferenceQueue.class, int.class, Object.class),
                                    nexusType.getMethod("clean", Reference.class));
                        } catch (Exception ignored) {
                            return new Dispatcher.Unavailable(exception);
                        }
                    }
                }
            }
        }

        /**
         * An enabled dispatcher for registering a type initializer in a {@link Nexus}.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Available implements Dispatcher {

            /**
             * Indicates that a static method is invoked by reflection.
             */
            private static final Object STATIC_METHOD = null;

            /**
             * The {@link Nexus#register(String, ClassLoader, ReferenceQueue, int, Object)} method.
             */
            private final Method register;

            /**
             * The {@link Nexus#clean(Reference)} method.
             */
            private final Method clean;

            /**
             * Creates a new dispatcher.
             *
             * @param register The {@link Nexus#register(String, ClassLoader, ReferenceQueue, int, Object)} method.
             * @param clean    The {@link Nexus#clean(Reference)} method.
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
            public void clean(Reference<? extends ClassLoader> reference) {
                try {
                    clean.invoke(STATIC_METHOD, reference);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access: " + clean, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke: " + clean, exception.getCause());
                }
            }

            @Override
            public void register(String name,
                                 ClassLoader classLoader,
                                 ReferenceQueue<? super ClassLoader> referenceQueue,
                                 int identification,
                                 LoadedTypeInitializer loadedTypeInitializer) {
                try {
                    register.invoke(STATIC_METHOD, name, classLoader, referenceQueue, identification, loadedTypeInitializer);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access: " + register, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke: " + register, exception.getCause());
                }
            }
        }

        /**
         * A disabled dispatcher where a {@link Nexus} is not available.
         */
        @HashCodeAndEqualsPlugin.Enhance
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
            public void clean(Reference<? extends ClassLoader> reference) {
                throw new IllegalStateException("Could not initialize Nexus accessor", exception);
            }

            @Override
            public void register(String name,
                                 ClassLoader classLoader,
                                 ReferenceQueue<? super ClassLoader> referenceQueue,
                                 int identification,
                                 LoadedTypeInitializer loadedTypeInitializer) {
                throw new IllegalStateException("Could not initialize Nexus accessor", exception);
            }
        }
    }
}
