package net.bytebuddy.dynamic.loading;

import net.bytebuddy.instrumentation.type.TypeDescription;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    protected final Map<String, byte[]> typeDefinitions;

    /**
     * The persistence handler of this class loader.
     */
    protected final PersistenceHandler persistenceHandler;

    /**
     * The access control context of this class loader's instantiation.
     */
    protected final AccessControlContext accessControlContext;

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
        accessControlContext = AccessController.getContext();
    }

    /**
     * Creates a new class loader for a given definition of classes.
     *
     * @param parent             The {@link java.lang.ClassLoader} that is the parent of this class loader.
     * @param typeDefinitions    A map of type descriptions pointing to their binary representations.
     * @param persistenceHandler The persistence handler to be used by the created class loader.
     * @param childFirst         {@code true} if the class loader should apply child first semantics when loading
     *                           the {@code typeDefinitions}.
     * @return A corresponding class loader.
     */
    public static ClassLoader of(ClassLoader parent,
                                 Map<TypeDescription, byte[]> typeDefinitions,
                                 PersistenceHandler persistenceHandler,
                                 boolean childFirst) {
        Map<String, byte[]> rawTypeDefinitions = new HashMap<String, byte[]>(typeDefinitions.size());
        for (Map.Entry<TypeDescription, byte[]> entry : typeDefinitions.entrySet()) {
            rawTypeDefinitions.put(entry.getKey().getName(), entry.getValue());
        }
        return childFirst
                ? new ChildFirst(parent, rawTypeDefinitions, persistenceHandler)
                : new ByteArrayClassLoader(parent, rawTypeDefinitions, persistenceHandler);
    }

    /**
     * Loads a given set of class descriptions and their binary representations.
     *
     * @param classLoader        The parent class loader.
     * @param types              The raw types to load.
     * @param persistenceHandler The persistence handler of the created class loader.
     * @param childFirst         {@code true} {@code true} if the created class loader should apply child-first
     *                           semantics when loading the {@code types}.
     * @return A map of the given type descriptions pointing to their loaded representations.
     */
    public static Map<TypeDescription, Class<?>> load(ClassLoader classLoader,
                                                      Map<TypeDescription, byte[]> types,
                                                      PersistenceHandler persistenceHandler,
                                                      boolean childFirst) {
        Map<TypeDescription, Class<?>> loadedTypes = new LinkedHashMap<TypeDescription, Class<?>>(types.size());
        classLoader = ByteArrayClassLoader.of(classLoader, types, persistenceHandler, childFirst);
        for (TypeDescription typeDescription : types.keySet()) {
            try {
                loadedTypes.put(typeDescription, classLoader.loadClass(typeDescription.getName()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Cannot load class " + typeDescription, e);
            }
        }
        return loadedTypes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            // This does not need synchronization because this method is only called from within
            // ClassLoader in a synchronized context.
            return AccessController.doPrivileged(new ClassLoadingAction(name), accessControlContext);
        } catch (PrivilegedActionException e) {
            throw (ClassNotFoundException) e.getCause();
        }
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
                "parent=" + getParent() +
                ", typeDefinitions=" + typeDefinitions +
                ", persistenceHandler=" + persistenceHandler +
                ", accessControlContext=" + accessControlContext +
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
        MANIFEST(true) {
            @Override
            protected byte[] lookup(String name, Map<String, byte[]> typeDefinitions) {
                return typeDefinitions.get(name);
            }

            @Override
            protected InputStream inputStream(String resourceName, Map<String, byte[]> typeDefinitions) {
                if (!resourceName.endsWith(CLASS_FILE_SUFFIX)) {
                    return null;
                }
                byte[] binaryRepresentation = typeDefinitions.get(resourceName.replace('/', '.')
                        .substring(0, resourceName.length() - CLASS_FILE_SUFFIX.length()));
                return binaryRepresentation == null
                        ? null
                        : new ByteArrayInputStream(binaryRepresentation);
            }
        },

        /**
         * The latent persistence handler hides all class file representations and does not make them accessible
         * even before they are loaded.
         */
        LATENT(false) {
            @Override
            protected byte[] lookup(String name, Map<String, byte[]> typeDefinitions) {
                return typeDefinitions.remove(name);
            }

            @Override
            protected InputStream inputStream(String resourceName, Map<String, byte[]> typeDefinitions) {
                return null;
            }
        };

        /**
         * The suffix of files in the Java class file format.
         */
        private static final String CLASS_FILE_SUFFIX = ".class";

        /**
         * {@code true} if this persistence handler represents manifest class file storage.
         */
        private final boolean manifest;

        /**
         * Creates a new persistence handler.
         *
         * @param manifest {@code true} if this persistence handler represents manifest class file storage.
         */
        private PersistenceHandler(boolean manifest) {
            this.manifest = manifest;
        }

        /**
         * Checks if this persistence handler represents manifest class file storage.
         *
         * @return {@code true} if this persistence handler represents manifest class file storage.
         */
        public boolean isManifest() {
            return manifest;
        }

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
         * @param resourceName    The resource name of the class to be exposed as its class file.
         * @param typeDefinitions A map of fully qualified class names pointing to their binary representations.
         * @return An input stream representing the requested resource or {@code null} if no such resource is known.
         */
        protected abstract InputStream inputStream(String resourceName, Map<String, byte[]> typeDefinitions);
    }

    /**
     * A {@link net.bytebuddy.dynamic.loading.ByteArrayClassLoader} which applies child-first semantics for the
     * given type definitions.
     */
    public static class ChildFirst extends ByteArrayClassLoader {

        /**
         * The suffix of files in the Java class file format.
         */
        private static final String CLASS_FILE_SUFFIX = ".class";

        /**
         * Creates a new child-first byte array class loader.
         *
         * @param parent             The {@link java.lang.ClassLoader} that is the parent of this class loader.
         * @param typeDefinitions    A map of fully qualified class names pointing to their binary representations.
         * @param persistenceHandler The persistence handler of this class loader.
         */
        public ChildFirst(ClassLoader parent,
                          Map<String, byte[]> typeDefinitions,
                          PersistenceHandler persistenceHandler) {
            super(parent, typeDefinitions, persistenceHandler);
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> type = findLoadedClass(name);
            if (type != null) {
                return type;
            }
            try {
                type = findClass(name);
                if (resolve) {
                    resolveClass(type);
                }
                return type;
            } catch (ClassNotFoundException e) {
                // If an unknown class is loaded, this implementation causes the findClass method of this instance
                // to be triggered twice. This is however of minor importance because this would result in a
                // ClassNotFoundException which is rather uncommon.
                return super.loadClass(name, resolve);
            }
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            InputStream inputStream = persistenceHandler.inputStream(name, typeDefinitions);
            // A non-persistent class must maintain to
            if (inputStream != null || (!persistenceHandler.isManifest() && isSelfDefined(name))) {
                return inputStream;
            } else {
                URL url = getResource(name);
                try {
                    return url != null ? url.openStream() : null;
                } catch (IOException ignored) {
                    return null;
                }
            }
        }

        /**
         * Checks if a resource name represents a class file of a class that was loaded by this class loader.
         *
         * @param resourceName The resource name of the class to be exposed as its class file.
         * @return {@code true} if this class represents a class that was already loaded by this class loader.
         */
        private boolean isSelfDefined(String resourceName) {
            if (!resourceName.endsWith(CLASS_FILE_SUFFIX)) {
                return false;
            }
            // This synchronization is required to avoid a racing condition to the actual class loading.
            synchronized (this) {
                String typeName = resourceName.replace('/', '.').substring(0, resourceName.length() - CLASS_FILE_SUFFIX.length());
                if (typeDefinitions.containsKey(typeName)) {
                    return true;
                }
                Class<?> loadedClass = findLoadedClass(typeName);
                return loadedClass != null && loadedClass.getClassLoader() == this;
            }
        }

        @Override
        public String toString() {
            return "ByteArrayClassLoader.ChildFirst{" +
                    "parent=" + getParent() +
                    ", typeDefinitions=" + typeDefinitions +
                    ", persistenceHandler=" + persistenceHandler +
                    ", accessControlContext=" + accessControlContext +
                    '}';
        }
    }

    /**
     * A class loading action is responsible to perform the loading of a class in a privileged security context.
     */
    private class ClassLoadingAction implements PrivilegedExceptionAction<Class<?>> {

        /**
         * The name of the type to be loaded.
         */
        private final String name;

        /**
         * Creates a new class loading action.
         *
         * @param name The name of the type to be loaded.
         */
        private ClassLoadingAction(String name) {
            this.name = name;
        }

        @Override
        public Class<?> run() throws ClassNotFoundException {
            byte[] javaType = persistenceHandler.lookup(name, typeDefinitions);
            if (javaType != null) {
                return defineClass(name, javaType, 0, javaType.length);
            }
            throw new ClassNotFoundException(name);
        }

        @Override
        public String toString() {
            return "ByteArrayClassLoader.ClassLoadingAction{name='" + name + "'}";
        }
    }
}
