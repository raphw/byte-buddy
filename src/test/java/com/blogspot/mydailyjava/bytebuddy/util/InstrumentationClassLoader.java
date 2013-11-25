package com.blogspot.mydailyjava.bytebuddy.util;

import org.objectweb.asm.ClassWriter;

public class InstrumentationClassLoader extends ClassLoader {

    public Class<?> loadFrom(ClassWriter classWriter) {
        byte[] bytes = classWriter.toByteArray();
        return defineClass(null, bytes, 0, bytes.length);
    }
}
