package net.bytebuddy.utility;

import net.bytebuddy.description.NamedElement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class JavaModule implements NamedElement.WithOptionalName, PrivilegedAction<ClassLoader> {

    public static JavaModule UNDEFINED = null;

    private static final Dispatcher DISPATCHER;

    static {
        Dispatcher dispatcher;
        try {
            Class<?> module = Class.forName("java.lang.reflect.Module");
            dispatcher = new Dispatcher.Enabled(Class.class.getDeclaredMethod("getModule"),
                    module.getDeclaredMethod("getClassLoader"),
                    module.getDeclaredMethod("isNamed"),
                    module.getDeclaredMethod("getName"));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception ignored) {
            dispatcher = Dispatcher.Disabled.INSTANCE;
        }
        DISPATCHER = dispatcher;
    }

    private final Object module;

    protected JavaModule(Object module) {
        this.module = module;
    }

    public static JavaModule ofType(Class<?> type) {
        return DISPATCHER.moduleOf(type);
    }

    public static JavaModule of(Object module) {
        if (!JavaType.MODULE.getTypeStub().isInstance(module)) {
            throw new IllegalArgumentException("Not a Java module: " + module);
        }
        return new JavaModule(module);
    }

    @Override
    public boolean isNamed() {
        return DISPATCHER.isNamed(module);
    }

    @Override
    public String getActualName() {
        return DISPATCHER.getName(module);
    }

    public Object unwrap() {
        return module;
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

    public ClassLoader getClassLoader(AccessControlContext accessControlContext) {
        return AccessController.doPrivileged(this, accessControlContext);
    }

    @Override
    public ClassLoader run() {
        return DISPATCHER.getClassLoader(module);
    }

    protected interface Dispatcher {

        JavaModule moduleOf(Class<?> type);

        boolean isNamed(Object module);

        String getName(Object module);

        ClassLoader getClassLoader(Object module);

        class Enabled implements Dispatcher {

            private final Method getModule;

            private final Method getClassLoader;

            private final Method isNamed;

            private final Method getName;

            protected Enabled(Method getModule, Method getClassLoader, Method isNamed, Method getName) {
                this.getModule = getModule;
                this.getClassLoader = getClassLoader;
                this.isNamed = isNamed;
                this.getName = getName;
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
        }

        enum Disabled implements Dispatcher {

            INSTANCE;

            @Override
            public JavaModule moduleOf(Class<?> type) {
                return UNDEFINED;
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
            public String toString() {
                return "JavaModule.Dispatcher.Disabled." + name();
            }
        }
    }
}
