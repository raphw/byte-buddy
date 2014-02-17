package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.loading;

import java.util.Map;

public class ByteArrayClassLoader extends ClassLoader {

    private final Map<String, byte[]> typeDefinitions;

    public ByteArrayClassLoader(ClassLoader parent, Map<String, byte[]> typeDefinitions) {
        super(parent);
        this.typeDefinitions = typeDefinitions;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        byte[] javaType = typeDefinitions.get(name);
        if (javaType != null) {
            return defineClass(name, javaType, 0, javaType.length);
        }
        return super.findClass(name);
    }
}
