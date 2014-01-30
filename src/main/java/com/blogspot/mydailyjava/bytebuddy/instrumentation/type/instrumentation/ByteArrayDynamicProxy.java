package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.instrumentation;

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
    public Class<? extends T> load(ClassLoader classLoader) {
        try {
            return (Class<? extends T>) Class.forName(typeName, false, new ByteArrayClassLoader(classLoader, typeName, javaType));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load class", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends T> loadReflective(ClassLoader classLoader) {
        return (Class<? extends T>) new ClassLoaderByteArrayInjector(classLoader).load(typeName, javaType);
    }
}
