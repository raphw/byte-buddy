package net.bytebuddy.dynamic.loading;

import net.bytebuddy.instrumentation.type.TypeDescription;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
     * The persistence handler of this class loader.
     */
    private final PersistenceHandler persistenceHandler;

    /**
     * Creates a new class loader for a given definition of classes.
     *
     * @param parent             The {@link java.lang.ClassLoader} that is the parent of this class loader.
     * @param typeDefinitions    A map of fully qualified class names pointing to their binary representations.
     * @param persistenceHandler The persistence handler of this class loader.
     */
    public ByteArrayClassLoader(ClassLoader parent,
                                Map<String, byte[]> typeDefinitions,
                                PersistenceHandler persistenceHandler) {
        super(parent);
        this.typeDefinitions = new HashMap<String, byte[]>(typeDefinitions);
        this.persistenceHandler = persistenceHandler;
    }

    /**
     * Creates a new class loader for a given definition of classes.
     *
     * @param parent             The {@link java.lang.ClassLoader} that is the parent of this class loader.
     * @param typeDefinitions    A map of type descriptions pointing to their binary representations.
     * @param persistenceHandler The persistence handler to be used by the created class loader.
     */
    public static ClassLoader of(ClassLoader parent,
                                 Map<TypeDescription, byte[]> typeDefinitions,
                                 PersistenceHandler persistenceHandler) {
        Map<String, byte[]> rawTypeDefinitions = new HashMap<String, byte[]>(typeDefinitions.size());
        for (Map.Entry<TypeDescription, byte[]> entry : typeDefinitions.entrySet()) {
            rawTypeDefinitions.put(entry.getKey().getName(), entry.getValue());
        }
        return new ByteArrayClassLoader(parent, rawTypeDefinitions, persistenceHandler);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Does not need synchronization because this method is only called from within
        // ClassLoader in a synchronized context.
        byte[] javaType = persistenceHandler.lookup(name, typeDefinitions);
        if (javaType != null) {
            return defineClass(name, javaType, 0, javaType.length);
        }
        return super.findClass(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream inputStream = super.getResourceAsStream(name);
        if (inputStream != null) {
            return inputStream;
        } else {
            return persistenceHandler.inputStream(name, typeDefinitions);
        }
    }

    @Override
    public String toString() {
        return "ByteArrayClassLoader{" +
                "typeDefinitions=" + typeDefinitions +
                ", persistenceHandler=" + persistenceHandler +
                '}';
    }

    /**
     * A persistence handler decides on weather the byte array that represents a loaded class is exposed by
     * the {@link java.lang.ClassLoader#getResourceAsStream(String)} method.
     */
    public static enum PersistenceHandler {

        /**
         * The manifest persistence handler retains all class file representations and makes them accessible.
         */
        MANIFEST {
            @Override
            protected byte[] lookup(String name, Map<String, byte[]> typeDefinitions) {
                return typeDefinitions.get(name);
            }

            @Override
            protected InputStream inputStream(String name, Map<String, byte[]> typeDefinitions) {
                byte[] binaryRepresentation = typeDefinitions.get(name);
                return binaryRepresentation == null
                        ? null
                        : new ByteArrayInputStream(binaryRepresentation);
            }
        },

        /**
         * The latent persistence handler hides all class file representations and does not make them accessible
         * even before they are loaded.
         */
        LATENT {
            @Override
            protected byte[] lookup(String name, Map<String, byte[]> typeDefinitions) {
                return typeDefinitions.remove(name);
            }

            @Override
            protected InputStream inputStream(String name, Map<String, byte[]> typeDefinitions) {
                return null;
            }
        };

        /**
         * Performs a lookup of a class file by its name.
         *
         * @param name            The name of the class to be loaded.
         * @param typeDefinitions A map of fully qualified class names pointing to their binary representations.
         * @return The byte array representing the requested class or {@code null} if no such class is known.
         */
        protected abstract byte[] lookup(String name, Map<String, byte[]> typeDefinitions);

        /**
         * Performs a lookup of an input stream for exposing a class file as a resource.
         *
         * @param name            The name of the class to be exposed as its class file.
         * @param typeDefinitions A map of fully qualified class names pointing to their binary representations.
         * @return An input stream representing the requested resource or {@code null} if no such resource is known.
         */
        protected abstract InputStream inputStream(String name, Map<String, byte[]> typeDefinitions);
    }
}
