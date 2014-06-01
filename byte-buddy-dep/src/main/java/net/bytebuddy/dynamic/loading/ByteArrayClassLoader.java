package net.bytebuddy.dynamic.loading;

import net.bytebuddy.instrumentation.type.TypeDescription;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link java.lang.ClassLoader} that is capable of loading explicitly defined classes. The class loader will free
 * any binary resources once a class that is defined by its binary data is loaded. This class loader is thread safe since
 * the class loading mechanics are only called from synchronized context.
 */
public class ByteArrayClassLoader extends ClassLoader {

    /**
     * A mutable map of type names mapped to their binary representation.
     */
    private final Map<String, byte[]> typeDefinitions;

    /**
     * Creates a new class loader for a given definition of classes.
     *
     * @param parent          The {@link java.lang.ClassLoader} that is the parent of this class loader.
     * @param typeDefinitions A map of fully qualified class names pointing to their binary representations.
     */
    public ByteArrayClassLoader(ClassLoader parent, Map<String, byte[]> typeDefinitions) {
        super(parent);
        this.typeDefinitions = new HashMap<String, byte[]>(typeDefinitions);
    }

    /**
     * Creates a new class loader for a given definition of classes.
     *
     * @param typeDefinitions A map of type descriptions pointing to their binary representations.
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
        // Does not need synchronization because this method is only called from within
        // ClassLoader in a synchronized context.
        byte[] javaType = typeDefinitions.remove(name);
        if (javaType != null) {
            return defineClass(name, javaType, 0, javaType.length);
        }
        return super.findClass(name);
    }

    @Override
    public String toString() {
        return "ByteArrayClassLoader{typeDefinitions=" + typeDefinitions + " (unloaded)}";
    }
}
