package com.blogspot.mydailyjava.bytebuddy.type.loading;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ClassLoaderByteArrayInjector {

    private static Method FIND_LOADED_CLASS_METHOD;
    private static Method LOAD_BYTE_ARRAY_METHOD;

    private static NoSuchMethodException EXCEPTION;

    static {
        try {
            FIND_LOADED_CLASS_METHOD = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            FIND_LOADED_CLASS_METHOD.setAccessible(true);
            LOAD_BYTE_ARRAY_METHOD = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            LOAD_BYTE_ARRAY_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            EXCEPTION = e;
        }
    }

    private final ClassLoader classLoader;

    public ClassLoaderByteArrayInjector(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Class<?> load(String name, byte[] javaType) {
        if (FIND_LOADED_CLASS_METHOD == null || LOAD_BYTE_ARRAY_METHOD == null) {
            throw new IllegalStateException("Could not find methods for class loader injection", EXCEPTION);
        }
        try {
            synchronized (classLoader) {
                Class<?> type = (Class<?>) FIND_LOADED_CLASS_METHOD.invoke(classLoader, name);
                if (type != null) {
                    return type;
                } else {
                    return (Class<?>) LOAD_BYTE_ARRAY_METHOD.invoke(classLoader, name, javaType, 0, javaType.length);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not access injection method", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Exception on invoking loader method", e.getCause());
        }
    }
}
