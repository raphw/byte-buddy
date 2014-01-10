package com.blogspot.mydailyjava.bytebuddy.type.scaffold;

import com.blogspot.mydailyjava.bytebuddy.DynamicProxy;
import com.blogspot.mydailyjava.bytebuddy.type.loading.ByteArrayClassLoader;
import com.blogspot.mydailyjava.bytebuddy.type.loading.ClassLoaderByteArrayInjector;

public class ByteArrayDynamicProxy implements DynamicProxy {

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
    public Class<?> load(ClassLoader classLoader) {
        try {
            return Class.forName(typeName, false, new ByteArrayClassLoader(classLoader, typeName, javaType));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load class", e);
        }
    }

    @Override
    public Class<?> loadReflective(ClassLoader classLoader) {
        return new ClassLoaderByteArrayInjector(classLoader).load(typeName, javaType);
    }
}
