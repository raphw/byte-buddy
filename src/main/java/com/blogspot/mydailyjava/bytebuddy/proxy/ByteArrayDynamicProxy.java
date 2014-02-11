package com.blogspot.mydailyjava.bytebuddy.proxy;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.loading.ByteArrayClassLoader;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.loading.ClassLoaderByteArrayInjector;

public class ByteArrayDynamicProxy<T> implements DynamicProxy<T> {

    private final String typeName;
    private final byte[] javaType;

    public ByteArrayDynamicProxy(String typeName, byte[] javaType) {
        this.typeName = typeName;
        this.javaType = javaType;
    }

    @Override
    public byte[] getBytes() {
        return javaType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public LoadedTypeDynamicProxy<T> load(ClassLoader classLoader) {
        try {
            Class<? extends T> type = (Class<? extends T>) Class.forName(typeName, false, new ByteArrayClassLoader(classLoader, typeName, javaType));
            return new LoadedTypeDynamicProxy<T>(type);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load class", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public LoadedTypeDynamicProxy<T> loadReflective(ClassLoader classLoader) {
        Class<? extends T> type = (Class<? extends T>) new ClassLoaderByteArrayInjector(classLoader).load(typeName, javaType);
        return new LoadedTypeDynamicProxy<T>(type);
    }
}
