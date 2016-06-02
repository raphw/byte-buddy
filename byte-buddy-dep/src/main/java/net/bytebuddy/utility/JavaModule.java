package net.bytebuddy.utility;

import net.bytebuddy.description.NamedElement;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Type-safe representation of a {@code java.lang.reflect.Module}. On platforms that do not support the module API, modules are represented by {@code null}.
 */
public class JavaModule implements NamedElement.WithOptionalName, PrivilegedAction<ClassLoader> {

    /**
     * Canonical representation of a Java module on a JVM that does not support the module API.
     */
    public static final JavaModule UNSUPPORTED = null;

    /**
     * The dispatcher to use for accessing Java modules, if available.
     */
    private static final Dispatcher DISPATCHER;

    /*
     * Extracts the dispatcher for Java modules that is supported by the current JVM.
     */
    static {
        Dispatcher dispatcher;
        try {
            Class<?> module = Class.forName("java.lang.reflect.Module");
            dispatcher = new Dispatcher.Enabled(Class.class.getDeclaredMethod("getModule"),
                    module.getDeclaredMethod("getClassLoader"),
                    module.getDeclaredMethod("isNamed"),
                    module.getDeclaredMethod("getName"),
                    module.getDeclaredMethod("canRead", module),
                    Instrumentation.class.getDeclaredMethod("addModuleReads", module, module));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception ignored) {
            dispatcher = Dispatcher.Disabled.INSTANCE;
        }
        DISPATCHER = dispatcher;
    }

    /**
     * The {@code java.lang.reflect.Module} instance this wrapper represents.
     */
    private final Object module;

    /**
     * Creates a new Java module representation.
     *
     * @param module The {@code java.lang.reflect.Module} instance this wrapper represents.
     */
    protected JavaModule(Object module) {
        this.module = module;
    }

    /**
     * Returns a representation of the supplied type's {@code java.lang.reflect.Module} or {@code null} if the current VM does not support modules.
     *
     * @param type The type for which to describe the module.
     * @return A representation of the type's module or {@code null} if the current VM does not support modules.
     */
    public static JavaModule ofType(Class<?> type) {
        return DISPATCHER.moduleOf(type);
    }

    /**
     * Represents the supplied {@code java.lang.reflect.Module} as an instance of this class and validates that the
     * supplied instance really represents a Java {@code Module}.
     *
     * @param module The module to represent.
     * @return A representation of the supplied Java module.
     */
    public static JavaModule of(Object module) {
        if (!JavaType.MODULE.getTypeStub().isInstance(module)) {
            throw new IllegalArgumentException("Not a Java module: " + module);
        }
        return new JavaModule(module);
    }

    /**
     * Checks if the current VM supports the {@code java.lang.reflect.Module} API.
     *
     * @return {@code true} if the current VM supports modules.
     */
    public static boolean isSupported() {
        return DISPATCHER.isAlive();
    }

    @Override
    public boolean isNamed() {
        return DISPATCHER.isNamed(module);
    }

    @Override
    public String getActualName() {
        return DISPATCHER.getName(module);
    }

    /**
     * Returns the class loader of this module.
     *
     * @param accessControlContext The access control context to use for using extracting the class loader.
     * @return The class loader of the represented module.
     */
    public ClassLoader getClassLoader(AccessControlContext accessControlContext) {
        return AccessController.doPrivileged(this, accessControlContext);
    }

    /**
     * Unwraps this instance to a {@code java.lang.reflect.Module}.
     *
     * @return The represented {@code java.lang.reflect.Module}.
     */
    public Object unwrap() {
        return module;
    }

    /**
     * Checks if this module can read the exported packages of the supplied module.
     *
     * @param module The module to check for its readability by this module.
     * @return {@code true} if this module can read the supplied module.
     */
    public boolean canRead(JavaModule module) {
        return DISPATCHER.canRead(this.module, module.unwrap());
    }

    /**
     * Adds a read-edge to this module to the supplied module using the instrumentation API.
     *
     * @param instrumentation The instrumentation instance to use for adding the edge.
     * @param module          The module to add as a read dependency to this module.
     */
    public void addReads(Instrumentation instrumentation, JavaModule module) {
        DISPATCHER.addReads(instrumentation, this.module, module.unwrap());
    }

    @Override
    public ClassLoader run() {
        return DISPATCHER.getClassLoader(module);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        JavaModule that = (JavaModule) object;
        return module.equals(that.module);
    }

    @Override
    public int hashCode() {
        return module.hashCode();
    }

    @Override
    public String toString() {
        return module.toString();
    }

    /**
     * A dispatcher for accessing the {@code java.lang.reflect.Module} API if it is available on the current VM.
     */
    protected interface Dispatcher {

        /**
         * Checks if this dispatcher is alive, i.e. supports modules.
         *
         * @return {@code true} if modules are supported on the current VM.
         */
        boolean isAlive();

        /**
         * Extracts the Java {@code Module} for the provided class or returns {@code null} if the current VM does not support modules.
         *
         * @param type The type for which to extract the module.
         * @return The class's {@code Module} or {@code null} if the current VM does not support modules.
         */
        JavaModule moduleOf(Class<?> type);

        /**
         * Returns {@code true} if the supplied module is named.
         *
         * @param module The {@code java.lang.reflect.Module} to check for the existence of a name.
         * @return {@code true} if the supplied module is named.
         */
        boolean isNamed(Object module);

        /**
         * Returns the module's name.
         *
         * @param module The {@code java.lang.reflect.Module} to check for its name.
         * @return The module's (implicit or explicit) name.
         */
        String getName(Object module);

        /**
         * Returns the module's class loader.
         *
         * @param module The {@code java.lang.reflect.Module}
         * @return The module's class loader.
         */
        ClassLoader getClassLoader(Object module);

        /**
         * Checks if the source module can read the target module.
         *
         * @param source The source module.
         * @param target The target module.
         * @return {@code true} if the source module can read the target module.
         */
        boolean canRead(Object source, Object target);

        /**
         * Adds a read-edge from the source to the target module.
         *
         * @param instrumentation The instrumentation instance to use for adding the edge.
         * @param source          The source module.
         * @param target          The target module.
         */
        void addReads(Instrumentation instrumentation, Object source, Object target);

        /**
         * A dispatcher for a VM that does support the {@code java.lang.reflect.Module} API.
         */
        class Enabled implements Dispatcher {

            /**
             * The {@code java.lang.Class#getModule()} method.
             */
            private final Method getModule;

            /**
             * The {@code java.lang.reflect.Module#getClassLoader()} method.
             */
            private final Method getClassLoader;

            /**
             * The {@code java.lang.reflect.Module#isNamed()} method.
             */
            private final Method isNamed;

            /**
             * The {@code java.lang.reflect.Module#getName()} method.
             */
            private final Method getName;

            /**
             * The {@code java.lang.reflect.Module#canRead(Module)} method.
             */
            private final Method canRead;

            /**
             * The {@code java.lang.instrument.Instrumentation#addModuleReads(Module, Module)} method.
             */
            private final Method addModuleReads;

            /**
             * Creates an enabled dispatcher.
             *
             * @param getModule      The {@code java.lang.Class#getModule()} method.
             * @param getClassLoader The {@code java.lang.reflect.Module#getClassLoader()} method.
             * @param isNamed        The {@code java.lang.reflect.Module#isNamed()} method.
             * @param getName        The {@code java.lang.reflect.Module#getName()} method.
             * @param canRead        The {@code java.lang.reflect.Module#canRead(Module)} method.
             * @param addModuleReads The {@code java.lang.instrument.Instrumentation#addModuleReads(Module, Module)} method.
             */
            protected Enabled(Method getModule, Method getClassLoader, Method isNamed, Method getName, Method canRead, Method addModuleReads) {
                this.getModule = getModule;
                this.getClassLoader = getClassLoader;
                this.isNamed = isNamed;
                this.getName = getName;
                this.canRead = canRead;
                this.addModuleReads = addModuleReads;
            }

            @Override
            public boolean isAlive() {
                return true;
            }

            @Override
            public JavaModule moduleOf(Class<?> type) {
                try {
                    return new JavaModule(getModule.invoke(type));
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + getModule, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + getModule, exception.getCause());
                }
            }

            @Override
            public ClassLoader getClassLoader(Object module) {
                try {
                    return (ClassLoader) getClassLoader.invoke(module);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + getClassLoader, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + getClassLoader, exception.getCause());
                }
            }

            @Override
            public boolean isNamed(Object module) {
                try {
                    return (Boolean) isNamed.invoke(module);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + isNamed, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + isNamed, exception.getCause());
                }
            }

            @Override
            public String getName(Object module) {
                try {
                    return (String) getName.invoke(module);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + getName, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + getName, exception.getCause());
                }
            }

            @Override
            public boolean canRead(Object source, Object target) {
                try {
                    return (Boolean) canRead.invoke(source, target);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + canRead, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + canRead, exception.getCause());
                }
            }

            @Override
            public void addReads(Instrumentation instrumentation, Object source, Object target) {
                try {
                    addModuleReads.invoke(instrumentation, source, target);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + addModuleReads, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + addModuleReads, exception.getCause());
                }
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                Enabled enabled = (Enabled) object;
                return getModule.equals(enabled.getModule)
                        && getClassLoader.equals(enabled.getClassLoader)
                        && isNamed.equals(enabled.isNamed)
                        && getName.equals(enabled.getName)
                        && canRead.equals(enabled.canRead)
                        && addModuleReads.equals(enabled.addModuleReads);
            }

            @Override
            public int hashCode() {
                int result = getModule.hashCode();
                result = 31 * result + getClassLoader.hashCode();
                result = 31 * result + isNamed.hashCode();
                result = 31 * result + getName.hashCode();
                result = 31 * result + canRead.hashCode();
                result = 31 * result + addModuleReads.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "JavaModule.Dispatcher.Enabled{" +
                        "getModule=" + getModule +
                        ", getClassLoader=" + getClassLoader +
                        ", isNamed=" + isNamed +
                        ", getName=" + getName +
                        ", canRead=" + canRead +
                        ", addModuleReads=" + addModuleReads +
                        '}';
            }
        }

        /**
         * A disabled dispatcher for a VM that does not support the {@code java.lang.reflect.Module} API.
         */
        enum Disabled implements Dispatcher {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public boolean isAlive() {
                return false;
            }

            @Override
            public JavaModule moduleOf(Class<?> type) {
                return UNSUPPORTED;
            }

            @Override
            public ClassLoader getClassLoader(Object module) {
                throw new IllegalStateException("Current VM does not support modules");
            }

            @Override
            public boolean isNamed(Object module) {
                throw new IllegalStateException("Current VM does not support modules");
            }

            @Override
            public String getName(Object module) {
                throw new IllegalStateException("Current VM does not support modules");
            }

            @Override
            public boolean canRead(Object source, Object target) {
                throw new IllegalStateException("Current VM does not support modules");
            }

            @Override
            public void addReads(Instrumentation instrumentation, Object source, Object target) {
                throw new IllegalStateException("Current VM does not support modules");
            }

            @Override
            public String toString() {
                return "JavaModule.Dispatcher.Disabled." + name();
            }
        }
    }
}
