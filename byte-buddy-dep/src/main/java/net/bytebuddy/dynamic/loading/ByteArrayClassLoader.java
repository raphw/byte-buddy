package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
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
     * Indicates that an array should be included from its first index. Improves the source code readability.
     */
    private static final int FROM_BEGINNING = 0;

    /**
     * Indicates that a lookup for a class file did not locate any class.
     */
    private static final InputStream NO_STREAM = null;

    /**
     * A mutable map of type names mapped to their binary representation.
     */
    protected final Map<String, byte[]> typeDefinitions;

    /**
     * The persistence handler of this class loader.
     */
    protected final PersistenceHandler persistenceHandler;

    /**
     * The protection domain to apply. Might be {@code null} when referencing the default protection domain.
     */
    protected final ProtectionDomain protectionDomain;

    /**
     * The package definer to be queried for package definitions.
     */
    protected final PackageDefinitionStrategy packageDefinitionStrategy;

    /**
     * The access control context to use for loading classes.
     */
    protected final AccessControlContext accessControlContext;

    /**
     * Creates a new class loader for a given definition of classes.
     *
     * @param parent                    The {@link java.lang.ClassLoader} that is the parent of this class loader.
     * @param typeDefinitions           A map of fully qualified class names pointing to their binary representations.
     * @param protectionDomain          The protection domain to apply where {@code null} references an implicit protection domain.
     * @param accessControlContext      The access control context to use for loading classes.
     * @param packageDefinitionStrategy The package definer to be queried for package definitions.
     * @param persistenceHandler        The persistence handler of this class loader.
     */
    public ByteArrayClassLoader(ClassLoader parent,
                                Map<String, byte[]> typeDefinitions,
                                ProtectionDomain protectionDomain,
                                AccessControlContext accessControlContext,
                                PersistenceHandler persistenceHandler,
                                PackageDefinitionStrategy packageDefinitionStrategy) {
        super(parent);
        this.typeDefinitions = new HashMap<String, byte[]>(typeDefinitions);
        this.protectionDomain = protectionDomain;
        this.accessControlContext = accessControlContext;
        this.persistenceHandler = persistenceHandler;
        this.packageDefinitionStrategy = packageDefinitionStrategy;
    }

    /**
     * Creates a new class loader for a given definition of classes.
     *
     * @param parent                    The {@link java.lang.ClassLoader} that is the parent of this class loader.
     * @param typeDefinitions           A map of type descriptions pointing to their binary representations.
     * @param protectionDomain          The protection domain to apply where {@code null} references an implicit protection domain.
     * @param accessControlContext      The access control context to use for loading classes.
     * @param persistenceHandler        The persistence handler to be used by the created class loader.
     * @param packageDefinitionStrategy The package definer to be queried for package definitions.
     * @param childFirst                {@code true} if the class loader should apply child first semantics when loading
     *                                  the {@code typeDefinitions}.
     * @return A corresponding class loader.
     */
    public static ClassLoader of(ClassLoader parent,
                                 Map<TypeDescription, byte[]> typeDefinitions,
                                 ProtectionDomain protectionDomain,
                                 AccessControlContext accessControlContext,
                                 PersistenceHandler persistenceHandler,
                                 PackageDefinitionStrategy packageDefinitionStrategy,
                                 boolean childFirst) {
        Map<String, byte[]> namedTypeDefinitions = new HashMap<String, byte[]>(typeDefinitions.size());
        for (Map.Entry<TypeDescription, byte[]> entry : typeDefinitions.entrySet()) {
            namedTypeDefinitions.put(entry.getKey().getName(), entry.getValue());
        }
        return AccessController.doPrivileged(new ClassLoaderCreationAction(parent,
                namedTypeDefinitions,
                protectionDomain,
                accessControlContext,
                persistenceHandler,
                packageDefinitionStrategy,
                childFirst), accessControlContext);
    }

    /**
     * Loads a given set of class descriptions and their binary representations.
     *
     * @param classLoader               The parent class loader.
     * @param types                     The unloaded types to be loaded.
     * @param protectionDomain          The protection domain to apply where {@code null} references an implicit protection domain.
     * @param accessControlContext      The access control context to use for loading classes.
     * @param persistenceHandler        The persistence handler of the created class loader.
     * @param packageDefinitionStrategy The package definer to be queried for package definitions.
     * @param childFirst                {@code true} {@code true} if the created class loader should apply child-first
     *                                  semantics when loading the {@code types}.
     * @return A map of the given type descriptions pointing to their loaded representations.
     */
    public static Map<TypeDescription, Class<?>> load(ClassLoader classLoader,
                                                      Map<TypeDescription, byte[]> types,
                                                      ProtectionDomain protectionDomain,
                                                      AccessControlContext accessControlContext,
                                                      PersistenceHandler persistenceHandler,
                                                      PackageDefinitionStrategy packageDefinitionStrategy,
                                                      boolean childFirst) {
        Map<TypeDescription, Class<?>> loadedTypes = new LinkedHashMap<TypeDescription, Class<?>>(types.size());
        classLoader = ByteArrayClassLoader.of(classLoader,
                types,
                protectionDomain,
                accessControlContext,
                persistenceHandler,
                packageDefinitionStrategy,
                childFirst);
        for (TypeDescription typeDescription : types.keySet()) {
            try {
                loadedTypes.put(typeDescription, classLoader.loadClass(typeDescription.getName()));
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Cannot load class " + typeDescription, exception);
            }
        }
        return loadedTypes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] javaType = persistenceHandler.lookup(name, typeDefinitions);
        if (javaType != null) {
            int packageIndex = name.lastIndexOf('.');
            if (packageIndex != -1) {
                String packageName = name.substring(0, packageIndex);
                PackageDefinitionStrategy.Definition definition = packageDefinitionStrategy.define(ByteArrayClassLoader.this, packageName, name);
                if (definition.isDefined()) {
                    Package definedPackage = getPackage(packageName);
                    if (definedPackage == null) {
                        definePackage(packageName,
                                definition.getSpecificationTitle(),
                                definition.getSpecificationVersion(),
                                definition.getSpecificationVendor(),
                                definition.getImplementationTitle(),
                                definition.getImplementationVersion(),
                                definition.getImplementationVendor(),
                                definition.getSealBase());
                    } else if (!definition.isCompatibleTo(definedPackage)) {
                        throw new SecurityException("Sealing violation for package " + packageName);
                    }
                }
            }
            return defineClass(name, javaType, FROM_BEGINNING, javaType.length, protectionDomain);
        }
        throw new ClassNotFoundException(name);
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
                ", protectionDomain=" + protectionDomain +
                ", packageDefinitionStrategy=" + packageDefinitionStrategy +
                ", accessControlContext=" + accessControlContext +
                '}';
    }

    /**
     * A persistence handler decides on weather the byte array that represents a loaded class is exposed by
     * the {@link java.lang.ClassLoader#getResourceAsStream(String)} method.
     */
    public enum PersistenceHandler {

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
                    return NO_STREAM;
                }
                byte[] binaryRepresentation = typeDefinitions.get(resourceName.replace('/', '.')
                        .substring(0, resourceName.length() - CLASS_FILE_SUFFIX.length()));
                return binaryRepresentation == null
                        ? NO_STREAM
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
                return NO_STREAM;
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
        PersistenceHandler(boolean manifest) {
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

        @Override
        public String toString() {
            return "ByteArrayClassLoader.PersistenceHandler." + name();
        }
    }

    /**
     * <p>
     * A {@link net.bytebuddy.dynamic.loading.ByteArrayClassLoader} which applies child-first semantics for the
     * given type definitions.
     * </p>
     * <p>
     * <b>Important</b>: Package definitions remain their parent-first semantics as loaded package definitions do not expose their class loaders.
     * </p>
     */
    public static class ChildFirst extends ByteArrayClassLoader {

        /**
         * The suffix of files in the Java class file format.
         */
        private static final String CLASS_FILE_SUFFIX = ".class";

        /**
         * Creates a new child-first byte array class loader.
         *
         * @param parent                    The {@link java.lang.ClassLoader} that is the parent of this class loader.
         * @param typeDefinitions           A map of fully qualified class names pointing to their binary representations.
         * @param protectionDomain          The protection domain to apply where {@code null} references an implicit protection domain.
         * @param accessControlContext      The access control context to use for loading classes.
         * @param persistenceHandler        The persistence handler of this class loader.
         * @param packageDefinitionStrategy The package definer to be queried for package definitions.
         */
        public ChildFirst(ClassLoader parent,
                          Map<String, byte[]> typeDefinitions,
                          ProtectionDomain protectionDomain,
                          AccessControlContext accessControlContext,
                          PersistenceHandler persistenceHandler,
                          PackageDefinitionStrategy packageDefinitionStrategy) {
            super(parent, typeDefinitions, protectionDomain, accessControlContext, persistenceHandler, packageDefinitionStrategy);
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
            } catch (ClassNotFoundException exception) {
                // If an unknown class is loaded, this implementation causes the findClass method of this instance
                // to be triggered twice. This is however of minor importance because this would result in a
                // ClassNotFoundException what does not alter the outcome.
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
                    return NO_STREAM;
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
                    ", protectionDomain=" + protectionDomain +
                    ", accessControlContext=" + accessControlContext +
                    ", persistenceHandler=" + persistenceHandler +
                    ", packageDefinitionStrategy=" + packageDefinitionStrategy +
                    '}';
        }
    }

    /**
     * An action for creating a class loader.
     */
    protected static class ClassLoaderCreationAction implements PrivilegedAction<ClassLoader> {

        /**
         * The {@link java.lang.ClassLoader} that is the parent of this class loader.
         */
        private final ClassLoader parent;

        /**
         * A map of fully qualified class names pointing to their binary representations.
         */
        private final Map<String, byte[]> typeDefinitions;

        /**
         * The protection domain to apply where {@code null} references an implicit protection domain.
         */
        private final ProtectionDomain protectionDomain;

        /**
         * The access control context to use for loading classes.
         */
        private final AccessControlContext accessControlContext;

        /**
         * The persistence handler of this class loader.
         */
        private final PersistenceHandler persistenceHandler;

        /**
         * The package definer to be queried for package definitions.
         */
        private final PackageDefinitionStrategy packageDefinitionStrategy;

        /**
         * {@code true} if this action should create a child-first class loader.
         */
        private final boolean childFirst;

        /**
         * Creates a new class loader creation action.
         *
         * @param parent                    The {@link java.lang.ClassLoader} that is the parent of this class loader.
         * @param typeDefinitions           A map of fully qualified class names pointing to their binary representations.
         * @param protectionDomain          The protection domain to apply where {@code null} references an implicit protection domain.
         * @param accessControlContext      The access control context to use for loading classes.
         * @param persistenceHandler        The persistence handler of this class loader.
         * @param packageDefinitionStrategy The package definer to be queried for package definitions.
         * @param childFirst                {@code true} if this action should create a child-first class loader.
         */
        protected ClassLoaderCreationAction(ClassLoader parent,
                                            Map<String, byte[]> typeDefinitions,
                                            ProtectionDomain protectionDomain,
                                            AccessControlContext accessControlContext,
                                            PersistenceHandler persistenceHandler,
                                            PackageDefinitionStrategy packageDefinitionStrategy,
                                            boolean childFirst) {
            this.parent = parent;
            this.typeDefinitions = typeDefinitions;
            this.protectionDomain = protectionDomain;
            this.accessControlContext = accessControlContext;
            this.persistenceHandler = persistenceHandler;
            this.packageDefinitionStrategy = packageDefinitionStrategy;
            this.childFirst = childFirst;
        }

        @Override
        public ClassLoader run() {
            return childFirst
                    ? new ChildFirst(parent, typeDefinitions, protectionDomain, accessControlContext, persistenceHandler, packageDefinitionStrategy)
                    : new ByteArrayClassLoader(parent, typeDefinitions, protectionDomain, accessControlContext, persistenceHandler, packageDefinitionStrategy);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            ClassLoaderCreationAction that = (ClassLoaderCreationAction) other;
            return childFirst == that.childFirst
                    && parent.equals(that.parent)
                    && typeDefinitions.equals(that.typeDefinitions)
                    && !(protectionDomain != null ? !protectionDomain.equals(that.protectionDomain) : that.protectionDomain != null)
                    && accessControlContext.equals(that.accessControlContext)
                    && persistenceHandler == that.persistenceHandler
                    && packageDefinitionStrategy.equals(that.packageDefinitionStrategy);
        }

        @Override
        public int hashCode() {
            int result = parent.hashCode();
            result = 31 * result + typeDefinitions.hashCode();
            result = 31 * result + (protectionDomain != null ? protectionDomain.hashCode() : 0);
            result = 31 * result + accessControlContext.hashCode();
            result = 31 * result + persistenceHandler.hashCode();
            result = 31 * result + packageDefinitionStrategy.hashCode();
            result = 31 * result + (childFirst ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ByteArrayClassLoader.ClassLoaderCreationAction{" +
                    "parent=" + parent +
                    ", typeDefinitions=" + typeDefinitions +
                    ", protectionDomain=" + protectionDomain +
                    ", accessControlContext=" + accessControlContext +
                    ", persistenceHandler=" + persistenceHandler +
                    ", packageDefinitionStrategy=" + packageDefinitionStrategy +
                    ", childFirst=" + childFirst +
                    '}';
        }
    }
}
