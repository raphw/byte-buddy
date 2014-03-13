package com.blogspot.mydailyjava.bytebuddy.dynamic.loading;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link java.lang.ClassLoader} that is capable of loading explicitly defined classes.
 */
public class ByteArrayClassLoader extends ClassLoader {

    private final Map<String, byte[]> typeDefinitions;

    /**
     * Creates a new class loader for a given definition of classes.
     *
     * @param parent          The {@link java.lang.ClassLoader} that is the parent of this class loader.
     * @param typeDefinitions A map of fully qualified class names to their {@code byte} definitions.
     */
    public ByteArrayClassLoader(ClassLoader parent, Map<String, byte[]> typeDefinitions) {
        super(parent);
        this.typeDefinitions = new HashMap<String, byte[]>(typeDefinitions);
    }

    /**
     * Creates a new class loader for a given definition of classes.
     *
     * @param typeDefinitions A map of type descriptions to their {@code byte} definitions.
     * @param parent          The {@link java.lang.ClassLoader} that is the parent of this class loader.
     */
    public ByteArrayClassLoader(Map<TypeDescription, byte[]> typeDefinitions, ClassLoader parent) {
        super(parent);
        this.typeDefinitions = new HashMap<String, byte[]>(typeDefinitions.size());
        for (Map.Entry<TypeDescription, byte[]> entry : typeDefinitions.entrySet()) {
            this.typeDefinitions.put(entry.getKey().getName(), entry.getValue());
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] javaType = typeDefinitions.get(name);
        if (javaType != null) {
            // Does not need synchronization because this method is only called from within
            // ClassLoader in a synchronized context.
            typeDefinitions.remove(name);
            return defineClass(name, javaType, 0, javaType.length);
        }
        return super.findClass(name);
    }
}
