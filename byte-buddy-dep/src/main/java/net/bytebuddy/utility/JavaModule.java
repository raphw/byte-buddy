package net.bytebuddy.utility;

import net.bytebuddy.description.NamedElement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JavaModule implements NamedElement.WithOptionalName {

    public static JavaModule UNDEFINED = null;

    private static final Dispatcher DISPATCHER;

    static {
        Dispatcher dispatcher;
        try {
            Class<?> module = Class.forName("java.lang.reflect.Module");
            dispatcher = new Dispatcher.Enabled(Class.class.getDeclaredMethod("getModule"),
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

    @Override
    public boolean isNamed() {
        return false;
    }

    @Override
    public String getActualName() {
        return null;
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

    protected interface Dispatcher {

        JavaModule moduleOf(Class<?> type);

        boolean isNamed(Object module);

        String getName(Object module);

        class Enabled implements Dispatcher {

            private final Method getModule;

            private final Method isNamed;

            private final Method getName;

            protected Enabled(Method getModule, Method isNamed, Method getName) {
                this.getModule = getModule;
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
            public boolean isNamed(Object module) {
                try {
                    return (Boolean) isNamed.invoke(module);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + getModule, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + getModule, exception.getCause());
                }
            }

            @Override
            public String getName(Object module) {
                try {
                    return (String) getName.invoke(module);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + getModule, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + getModule, exception.getCause());
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
