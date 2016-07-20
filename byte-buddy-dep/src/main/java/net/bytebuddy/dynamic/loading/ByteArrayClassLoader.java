package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.*;

/**
 * <p>
 * A {@link java.lang.ClassLoader} that is capable of loading explicitly defined classes. The class loader will free
 * any binary resources once a class that is defined by its binary data is loaded. This class loader is thread safe since
 * the class loading mechanics are only called from synchronized context.
 * </p>
 * <p>
 * <b>Note</b>: Instances of this class loader return URLs for their represented class loaders with the <i>bytebuddy</i> schema.
 * These URLs do not represent URIs as two classes with the same name yield identical URLs but might represents different byte
 * arrays.
 * </p>
 */
public class ByteArrayClassLoader extends ClassLoader {

    /**
     * The schema for URLs that represent a class file of byte array class loaders.
     */
    public static final String URL_SCHEMA = "bytebuddy";

    /**
     * Indicates that an array should be included from its first index. Improves the source code readability.
     */
    private static final int FROM_BEGINNING = 0;

    /**
     * Indicates that a URL does not exist to improve code readability.
     */
    private static final URL NO_URL = null;

    /**
     * A strategy for locating a package by name.
     */
    private static final PackageLookupStrategy PACKAGE_LOOKUP_STRATEGY;

    /*
     * Locates the best available package lookup strategy.
     */
    static {
        PackageLookupStrategy packageLookupStrategy;
        try {
            packageLookupStrategy = new PackageLookupStrategy.ForJava9CapableVm(ClassLoader.class.getDeclaredMethod("getDefinedPackage", String.class));
        } catch (NoSuchMethodException ignored) {
            packageLookupStrategy = PackageLookupStrategy.ForLegacyVm.INSTANCE;
        }
        PACKAGE_LOOKUP_STRATEGY = packageLookupStrategy;
    }

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
        Map<String, byte[]> namedTypeDefinitions = new HashMap<String, byte[]>();
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
     * @param childFirst                {@code true} if the created class loader should apply child-first semantics when loading the {@code types}.
     * @param forbidExisting            {@code true} if the class loading should throw an exception if a class was already loaded by a parent class loader.
     * @return A map of the given type descriptions pointing to their loaded representations.
     */
    public static Map<TypeDescription, Class<?>> load(ClassLoader classLoader,
                                                      Map<TypeDescription, byte[]> types,
                                                      ProtectionDomain protectionDomain,
                                                      AccessControlContext accessControlContext,
                                                      PersistenceHandler persistenceHandler,
                                                      PackageDefinitionStrategy packageDefinitionStrategy,
                                                      boolean childFirst,
                                                      boolean forbidExisting) {
        Map<TypeDescription, Class<?>> loadedTypes = new LinkedHashMap<TypeDescription, Class<?>>();
        classLoader = ByteArrayClassLoader.of(classLoader,
                types,
                protectionDomain,
                accessControlContext,
                persistenceHandler,
                packageDefinitionStrategy,
                childFirst);
        for (TypeDescription typeDescription : types.keySet()) {
            try {
                Class<?> type = Class.forName(typeDescription.getName(), false, classLoader);
                if (forbidExisting && type.getClassLoader() != classLoader) {
                    throw new IllegalStateException("Class already loaded: " + type);
                }
                loadedTypes.put(typeDescription, type);
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
                    Package definedPackage = PACKAGE_LOOKUP_STRATEGY.apply(this, packageName);
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
    protected URL findResource(String name) {
        return persistenceHandler.url(name, typeDefinitions, accessControlContext);
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        URL url = persistenceHandler.url(name, typeDefinitions, accessControlContext);
        return url == null
                ? EmptyEnumeration.INSTANCE
                : new SingletonEnumeration(url);
    }

    /**
     * Returns the package for a given name.
     *
     * @param name The name of the package.
     * @return A suitable package or {@code null} if no such package exists.
     */
    @SuppressWarnings("deprecation")
    private Package doGetPackage(String name) {
        return getPackage(name);
    }

    @Override
    public String toString() {
        return "ByteArrayClassLoader{" +
                "typeDefinitions=" + typeDefinitions +
                ", persistenceHandler=" + persistenceHandler +
                ", protectionDomain=" + protectionDomain +
                ", packageDefinitionStrategy=" + packageDefinitionStrategy +
                ", accessControlContext=" + accessControlContext +
                '}';
    }

    /**
     * A package lookup strategy for locating a package by name.
     */
    protected interface PackageLookupStrategy {

        /**
         * Returns a package for a given byte array class loader and a name.
         *
         * @param classLoader The class loader to locate a package for.
         * @param name        The name of the package.
         * @return A suitable package or {@code null} if no such package exists.
         */
        Package apply(ByteArrayClassLoader classLoader, String name);

        /**
         * A package lookup strategy for a VM prior to Java 9.
         */
        enum ForLegacyVm implements PackageLookupStrategy {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Package apply(ByteArrayClassLoader classLoader, String name) {
                return classLoader.doGetPackage(name);
            }

            @Override
            public String toString() {
                return "ByteArrayClassLoader.PackageLookupStrategy.ForLegacyVm." + name();
            }
        }

        /**
         * A package lookup strategy for Java 9 or newer.
         */
        class ForJava9CapableVm implements PackageLookupStrategy {

            /**
             * The {@code java.lang.ClassLoader#getDefinedPackage(String)} method.
             */
            private final Method getDefinedPackage;

            /**
             * Creates a new package lookup strategy for a modern VM.
             *
             * @param getDefinedPackage The {@code java.lang.ClassLoader#getDefinedPackage(String)} method.
             */
            protected ForJava9CapableVm(Method getDefinedPackage) {
                this.getDefinedPackage = getDefinedPackage;
            }

            @Override
            public Package apply(ByteArrayClassLoader classLoader, String name) {
                try {
                    return (Package) getDefinedPackage.invoke(classLoader, name);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + getDefinedPackage, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + getDefinedPackage, exception.getCause());
                }
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ForJava9CapableVm that = (ForJava9CapableVm) object;
                return getDefinedPackage.equals(that.getDefinedPackage);
            }

            @Override
            public int hashCode() {
                return getDefinedPackage.hashCode();
            }

            @Override
            public String toString() {
                return "ByteArrayClassLoader.PackageLookupStrategy.ForJava9CapableVm{" +
                        "getDefinedPackage=" + getDefinedPackage +
                        '}';
            }
        }
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
            protected URL url(String resourceName, Map<String, byte[]> typeDefinitions, AccessControlContext accessControlContext) {
                if (!resourceName.endsWith(CLASS_FILE_SUFFIX)) {
                    return NO_URL;
                } else if (resourceName.startsWith("/")) {
                    resourceName = resourceName.substring(1);
                }
                String typeName = resourceName.replace('/', '.').substring(FROM_BEGINNING, resourceName.length() - CLASS_FILE_SUFFIX.length());
                byte[] binaryRepresentation = typeDefinitions.get(typeName);
                return binaryRepresentation == null
                        ? NO_URL
                        : AccessController.doPrivileged(new UrlDefinitionAction(resourceName, binaryRepresentation), accessControlContext);

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
            protected URL url(String resourceName, Map<String, byte[]> typeDefinitions, AccessControlContext accessControlContext) {
                return NO_URL;
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
         * Returns a URL representing a class file.
         *
         * @param resourceName         The name of the requested resource.
         * @param typeDefinitions      A mapping of byte arrays by their type names.
         * @param accessControlContext The access control context to be used for creating the URL.
         * @return A URL representing the type definition or {@code null} if the requested resource does not represent a class file.
         */
        protected abstract URL url(String resourceName, Map<String, byte[]> typeDefinitions, AccessControlContext accessControlContext);

        @Override
        public String toString() {
            return "ByteArrayClassLoader.PersistenceHandler." + name();
        }

        /**
         * An action to define a URL that represents a class file.
         */
        protected static class UrlDefinitionAction implements PrivilegedAction<URL> {

            /**
             * The URL's encoding character set.
             */
            private static final String ENCODING = "UTF-8";

            /**
             * A value to define a standard port as Byte Buddy's URLs do not represent a port.
             */
            private static final int NO_PORT = -1;

            /**
             * Indicates that Byte Buddy's URLs do not have a file segment.
             */
            private static final String NO_FILE = "";

            /**
             * The name of the type that this URL represents.
             */
            private final String typeName;

            /**
             * The binary representation of the type's class file.
             */
            private final byte[] binaryRepresentation;

            /**
             * Creates a new URL definition action.
             *
             * @param typeName             The name of the type that this URL represents.
             * @param binaryRepresentation The binary representation of the type's class file.
             */
            protected UrlDefinitionAction(String typeName, byte[] binaryRepresentation) {
                this.typeName = typeName;
                this.binaryRepresentation = binaryRepresentation;
            }

            @Override
            public URL run() {
                try {
                    return new URL(URL_SCHEMA,
                            URLEncoder.encode(typeName.replace('.', '/'), ENCODING),
                            NO_PORT,
                            NO_FILE,
                            new ByteArrayUrlStreamHandler(binaryRepresentation));
                } catch (MalformedURLException exception) {
                    throw new IllegalStateException("Cannot create URL for " + typeName, exception);
                } catch (UnsupportedEncodingException exception) {
                    throw new IllegalStateException("Could not find encoding: " + ENCODING, exception);
                }
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                UrlDefinitionAction that = (UrlDefinitionAction) other;
                return typeName.equals(that.typeName) && Arrays.equals(binaryRepresentation, that.binaryRepresentation);
            }

            @Override
            public int hashCode() {
                int result = typeName.hashCode();
                result = 31 * result + Arrays.hashCode(binaryRepresentation);
                return result;
            }

            @Override
            public String toString() {
                return "ByteArrayClassLoader.PersistenceHandler.UrlDefinitionAction{" +
                        "typeName='" + typeName + '\'' +
                        "binaryRepresentation=<" + binaryRepresentation.length + " bytes>" +
                        '}';
            }

            /**
             * A stream handler that returns the given binary representation.
             */
            protected static class ByteArrayUrlStreamHandler extends URLStreamHandler {

                /**
                 * The binary representation of a type's class file.
                 */
                private final byte[] binaryRepresentation;

                /**
                 * Creates a new byte array URL stream handler.
                 *
                 * @param binaryRepresentation The binary representation of a type's class file.
                 */
                protected ByteArrayUrlStreamHandler(byte[] binaryRepresentation) {
                    this.binaryRepresentation = binaryRepresentation;
                }

                @Override
                protected URLConnection openConnection(URL url) throws IOException {
                    return new ByteArrayUrlConnection(url, new ByteArrayInputStream(binaryRepresentation));
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    ByteArrayUrlStreamHandler that = (ByteArrayUrlStreamHandler) other;
                    return Arrays.equals(binaryRepresentation, that.binaryRepresentation);
                }

                @Override
                public int hashCode() {
                    return Arrays.hashCode(binaryRepresentation);
                }

                @Override
                public String toString() {
                    return "ByteArrayClassLoader.PersistenceHandler.UrlDefinitionAction.ByteArrayUrlStreamHandler{" +
                            "binaryRepresentation=<" + binaryRepresentation.length + " bytes>" +
                            '}';
                }

                /**
                 * A URL connection for a given byte array.
                 */
                protected static class ByteArrayUrlConnection extends URLConnection {

                    /**
                     * The input stream to return for this connection.
                     */
                    private final InputStream inputStream;

                    /**
                     * Creates a new byte array URL connection.
                     *
                     * @param url         The URL that this connection represents.
                     * @param inputStream The input stream to return from this connection.
                     */
                    protected ByteArrayUrlConnection(URL url, InputStream inputStream) {
                        super(url);
                        this.inputStream = inputStream;
                    }

                    @Override
                    public void connect() {
                        connected = true;
                    }

                    @Override
                    public InputStream getInputStream() {
                        connect(); // Mimics the semantics of an actual URL connection.
                        return inputStream;
                    }

                    @Override
                    public String toString() {
                        return "ByteArrayClassLoader.PersistenceHandler.UrlDefinitionAction.ByteArrayUrlStreamHandler.ByteArrayUrlConnection{" +
                                "inputStream=" + inputStream +
                                '}';
                    }
                }
            }
        }
    }

    /**
     * <p>
     * A {@link net.bytebuddy.dynamic.loading.ByteArrayClassLoader} which applies child-first semantics for the
     * given type definitions.
     * </p>
     * <p>
     * <b>Important</b>: Package definitions remain their parent-first semantics as loaded package definitions do not expose their class loaders.
     * Also, it is not possible to make this class or its subclass parallel-capable as the loading strategy is overridden.
     * </p>
     */
    public static class ChildFirst extends ByteArrayClassLoader {

        /**
         * The suffix of files in the Java class file format.
         */
        private static final String CLASS_FILE_SUFFIX = ".class";

        /**
         * The synchronization engine for the executing JVM.
         */
        private static final SynchronizationStrategy SYNCHRONIZATION_STRATEGY;

        /*
         * Sets up the suitable synchronization engine (Java 8+ or earlier).
         */
        static {
            SynchronizationStrategy synchronizationStrategy;
            try {
                synchronizationStrategy = SynchronizationStrategy.ForJava7CapableVm.resolve();
            } catch (Exception ignored) {
                synchronizationStrategy = SynchronizationStrategy.ForLegacyVm.INSTANCE;
            }
            SYNCHRONIZATION_STRATEGY = synchronizationStrategy;
        }

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
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (SYNCHRONIZATION_STRATEGY.classLoadingLock(name, this)) {
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
        }

        @Override
        public URL getResource(String name) {
            URL url = persistenceHandler.url(name, typeDefinitions, accessControlContext);
            // If a class resource is defined by this class loader but it is not defined in a manifest manner,
            // the resource of the parent class loader should be shadowed by 'null'. Note that the delegation
            // model causes a redundant query to the persistent handler but renders a correct result.
            return url != null || isShadowed(name)
                    ? url
                    : super.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            URL url = persistenceHandler.url(name, typeDefinitions, accessControlContext);
            return url == null
                    ? super.getResources(name)
                    : new PrependingEnumeration(url, super.getResources(name));
        }

        /**
         * Checks if a resource name represents a class file of a class that was loaded by this class loader.
         *
         * @param resourceName The resource name of the class to be exposed as its class file.
         * @return {@code true} if this class represents a class that is being loaded by this class loader.
         */
        private boolean isShadowed(String resourceName) {
            if (persistenceHandler.isManifest() || !resourceName.endsWith(CLASS_FILE_SUFFIX)) {
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
                    "typeDefinitions=" + typeDefinitions +
                    ", protectionDomain=" + protectionDomain +
                    ", accessControlContext=" + accessControlContext +
                    ", persistenceHandler=" + persistenceHandler +
                    ", packageDefinitionStrategy=" + packageDefinitionStrategy +
                    '}';
        }

        /**
         * An engine for receiving a <i>class loading lock</i> when loading a class.
         */
        protected interface SynchronizationStrategy {

            /**
             * Receives the class loading lock.
             *
             * @param name        The name of the class being loaded.
             * @param classLoader The class loader loading the class.
             * @return The corresponding class loading lock.
             */
            Object classLoadingLock(String name, ClassLoader classLoader);

            /**
             * A synchronization engine for a VM that is not aware of parallel-capable class loaders.
             */
            enum ForLegacyVm implements SynchronizationStrategy {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Object classLoadingLock(String name, ClassLoader classLoader) {
                    return classLoader;
                }

                @Override
                public String toString() {
                    return "ByteArrayClassLoader.ChildFirst.SynchronizationStrategy.ForLegacyVm." + name();
                }
            }

            /**
             * A synchronization engine for a VM that is aware of parallel-capable class loaders.
             */
            class ForJava7CapableVm implements SynchronizationStrategy, PrivilegedAction<SynchronizationStrategy> {

                /**
                 * The {@code ClassLoader#getClassLoadingLock(String)} method.
                 */
                private final Method method;

                /**
                 * Creates a new synchronization engine.
                 *
                 * @param method The {@code ClassLoader#getClassLoadingLock(String)} method.
                 */
                protected ForJava7CapableVm(Method method) {
                    this.method = method;
                }

                /**
                 * Resolves a synchronization engine for a modern VM if this is possible.
                 *
                 * @return A modern synchronization engine.
                 * @throws NoSuchMethodException If the executing VM is not a modern VM.
                 */
                protected static SynchronizationStrategy resolve() throws NoSuchMethodException {
                    return AccessController.doPrivileged(new ForJava7CapableVm(ClassLoader.class.getDeclaredMethod("getClassLoadingLock", String.class)));
                }

                @Override
                public Object classLoadingLock(String name, ClassLoader classLoader) {
                    try {
                        return method.invoke(classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access class loading lock for " + name + " on " + classLoader, exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error when getting " + name + " on " + classLoader, exception);
                    }
                }

                @Override
                public SynchronizationStrategy run() {
                    method.setAccessible(true);
                    return this;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && method.equals(((ForJava7CapableVm) other).method);
                }

                @Override
                public int hashCode() {
                    return method.hashCode();
                }

                @Override
                public String toString() {
                    return "ByteArrayClassLoader.ChildFirst.SynchronizationStrategy.ForJava7CapableVm{method=" + method + '}';
                }
            }
        }

        /**
         * An enumeration that prepends an element to another enumeration and skips the last element of the provided enumeration.
         */
        protected static class PrependingEnumeration implements Enumeration<URL> {

            /**
             * The next element to return from this enumeration or {@code null} if such an element does not exist.
             */
            private URL nextElement;

            /**
             * The enumeration from which the next elements should be pulled.
             */
            private final Enumeration<URL> enumeration;

            /**
             * Creates a new prepending enumeration.
             *
             * @param url         The first element of the enumeration.
             * @param enumeration An enumeration that is used for pulling subsequent urls.
             */
            protected PrependingEnumeration(URL url, Enumeration<URL> enumeration) {
                nextElement = url;
                this.enumeration = enumeration;
            }

            @Override
            public boolean hasMoreElements() {
                return nextElement != null && enumeration.hasMoreElements();
            }

            @Override
            public URL nextElement() {
                if (nextElement != null && enumeration.hasMoreElements()) {
                    try {
                        return nextElement;
                    } finally {
                        nextElement = enumeration.nextElement();
                    }
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public String toString() {
                return "ByteArrayClassLoader.ChildFirst.PrependingEnumeration{" +
                        "nextElement=" + nextElement +
                        ", enumeration=" + enumeration +
                        '}';
            }
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

    /**
     * An enumeration without any elements.
     */
    protected enum EmptyEnumeration implements Enumeration<URL> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public URL nextElement() {
            throw new NoSuchElementException();
        }

        @Override
        public String toString() {
            return "ByteArrayClassLoader.EmptyEnumeration." + name();
        }
    }

    /**
     * An enumeration that contains a single element.
     */
    protected static class SingletonEnumeration implements Enumeration<URL> {

        /**
         * The current element or {@code null} if this enumeration does not contain further elements.
         */
        private URL element;

        /**
         * Creates a new singleton enumeration.
         *
         * @param element The only element.
         */
        protected SingletonEnumeration(URL element) {
            this.element = element;
        }

        @Override
        public boolean hasMoreElements() {
            return element != null;
        }

        @Override
        public URL nextElement() {
            if (element == null) {
                throw new NoSuchElementException();
            } else {
                try {
                    return element;
                } finally {
                    element = null;
                }
            }
        }

        @Override
        public String toString() {
            return "ByteArrayClassLoader.SingletonEnumeration{" +
                    "element=" + element +
                    '}';
        }
    }
}
