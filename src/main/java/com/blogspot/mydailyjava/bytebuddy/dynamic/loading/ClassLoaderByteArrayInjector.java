package com.blogspot.mydailyjava.bytebuddy.dynamic.loading;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An injector that loads classes by reflectively invoking non-public methods on a given {@link java.lang.ClassLoader}.
 * <p/>
 * Note that the injector is only able to load classes in a linear fashion. Thus, classes that refer to other classes
 * which are not yet loaded cannot be injected but will result in a {@link java.lang.NoClassDefFoundError}. This becomes
 * a problem when unloaded classes refer to each other using cyclic references. This injector cannot be applied to the
 * bootstrap class loader which is usually represented by {@code null} and therefore cannot be accessed by reflection.
 */
public class ClassLoaderByteArrayInjector {

    private static Method FIND_LOADED_CLASS_METHOD;
    private static Method LOAD_BYTE_ARRAY_METHOD;

    private static Exception EXCEPTION;

    static {
        try {
            FIND_LOADED_CLASS_METHOD = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            FIND_LOADED_CLASS_METHOD.setAccessible(true);
            LOAD_BYTE_ARRAY_METHOD = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            LOAD_BYTE_ARRAY_METHOD.setAccessible(true);
        } catch (Exception e) {
            EXCEPTION = e;
        }
    }

    private final ClassLoader classLoader;

    /**
     * Creates a new injector for the given {@link java.lang.ClassLoader}.
     *
     * @param classLoader The {@link java.lang.ClassLoader} into which new class definitions are injected.
     */
    public ClassLoaderByteArrayInjector(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Explicitly loads a {@link java.lang.Class} by using reflection.
     *
     * @param name       The fully qualified internalName of the {@link java.lang.Class} to be loaded.
     * @param definition The type's definition.
     * @return The loaded class.
     */
    public Class<?> inject(String name, byte[] definition) {
        if (FIND_LOADED_CLASS_METHOD == null || LOAD_BYTE_ARRAY_METHOD == null) {
            throw new IllegalStateException("Could not initialize class loader injector", EXCEPTION);
        }
        try {
            synchronized (classLoader) {
                Class<?> type = (Class<?>) FIND_LOADED_CLASS_METHOD.invoke(classLoader, name);
                if (type != null) {
                    return type;
                } else {
                    return (Class<?>) LOAD_BYTE_ARRAY_METHOD.invoke(classLoader, name, definition, 0, definition.length);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not access injection method", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Exception on invoking loader method", e.getCause());
        }
    }
}
