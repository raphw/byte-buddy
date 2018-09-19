package net.bytebuddy.dynamic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.JavaType;
import net.bytebuddy.utility.StreamDrainer;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.bytebuddy.matcher.ElementMatchers.isChildOf;

/**
 * Locates a class file or its byte array representation when it is given its type description.
 */
public interface ClassFileLocator extends Closeable {

    /**
     * The file extension for a Java class file.
     */
    String CLASS_FILE_EXTENSION = ".class";

    /**
     * Locates the class file for a given type and returns the binary data of the class file.
     *
     * @param name The name of the type to locate a class file representation for.
     * @return Any binary representation of the type which might be illegal.
     * @throws java.io.IOException If reading a class file causes an error.
     */
    Resolution locate(String name) throws IOException;

    /**
     * Represents a class file as binary data.
     */
    interface Resolution {

        /**
         * Checks if this binary representation is valid.
         *
         * @return {@code true} if this binary representation is valid.
         */
        boolean isResolved();

        /**
         * Finds the data of this binary representation. Calling this method is only legal for resolved instances.
         * For non-resolved instances, an exception is thrown.
         *
         * @return The requested binary data. The returned array must not be altered.
         */
        byte[] resolve();

        /**
         * A canonical representation of an illegal binary representation.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Illegal implements Resolution {

            /**
             * The name of the unresolved class file.
             */
            private final String typeName;

            /**
             * Creates an illegal resolution for a class file.
             *
             * @param typeName The name of the unresolved class file.
             */
            public Illegal(String typeName) {
                this.typeName = typeName;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isResolved() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public byte[] resolve() {
                throw new IllegalStateException("Could not locate class file for " + typeName);
            }
        }

        /**
         * Represents a byte array as binary data.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Explicit implements Resolution {

            /**
             * The represented data.
             */
            private final byte[] binaryRepresentation;

            /**
             * Creates a new explicit resolution of a given array of binary data.
             *
             * @param binaryRepresentation The binary data to represent. The array must not be modified.
             */
            @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The array is not to be modified by contract")
            public Explicit(byte[] binaryRepresentation) {
                this.binaryRepresentation = binaryRepresentation;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isResolved() {
                return true;
            }

            /**
             * {@inheritDoc}
             */
            @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The array is not to be modified by contract")
            public byte[] resolve() {
                return binaryRepresentation;
            }
        }
    }

    /**
     * A class file locator that cannot locate any class files.
     */
    enum NoOp implements ClassFileLocator {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) {
            return new Resolution.Illegal(name);
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }
    }

    /**
     * A simple class file locator that returns class files from a selection of given types.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Simple implements ClassFileLocator {

        /**
         * The class files that are known to this class file locator mapped by their type name.
         */
        private final Map<String, byte[]> classFiles;

        /**
         * Creates a new simple class file locator.
         *
         * @param classFiles The class files that are known to this class file locator mapped by their type name.
         */
        public Simple(Map<String, byte[]> classFiles) {
            this.classFiles = classFiles;
        }

        /**
         * Creates a class file locator for a single known type.
         *
         * @param typeName             The name of the type.
         * @param binaryRepresentation The binary representation of the type.
         * @return An appropriate class file locator.
         */
        public static ClassFileLocator of(String typeName, byte[] binaryRepresentation) {
            return new Simple(Collections.singletonMap(typeName, binaryRepresentation));
        }

        /**
         * Creates a class file locator that represents all types of a dynamic type.
         *
         * @param dynamicType The dynamic type to represent.
         * @return A class file locator representing the dynamic type's types.
         */
        public static ClassFileLocator of(DynamicType dynamicType) {
            return of(dynamicType.getAllTypes());
        }

        /**
         * Creates a class file locator that represents all types of a dynamic type.
         *
         * @param binaryRepresentations The binary representation of all types.
         * @return A class file locator representing the dynamic type's types.
         */
        public static ClassFileLocator of(Map<TypeDescription, byte[]> binaryRepresentations) {
            Map<String, byte[]> classFiles = new HashMap<String, byte[]>();
            for (Map.Entry<TypeDescription, byte[]> entry : binaryRepresentations.entrySet()) {
                classFiles.put(entry.getKey().getName(), entry.getValue());
            }
            return new Simple(classFiles);
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) {
            byte[] binaryRepresentation = classFiles.get(name);
            return binaryRepresentation == null
                    ? new Resolution.Illegal(name)
                    : new Resolution.Explicit(binaryRepresentation);
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }
    }

    /**
     * <p>
     * A class file locator that queries a class loader for binary representations of class files.
     * </p>
     * <p>
     * <b>Important</b>: Even when calling {@link Closeable#close()} on this class file locator, no underlying
     * class loader is closed if it implements the {@link Closeable} interface as this is typically not intended.
     * </p>
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForClassLoader implements ClassFileLocator {

        /**
         * The class loader to query.
         */
        private final ClassLoader classLoader;

        /**
         * Creates a new class file locator for the given class loader.
         *
         * @param classLoader The class loader to query which must not be the bootstrap class loader, i.e. {@code null}.
         */
        protected ForClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        /**
         * Creates a class file locator that queries the system class loader.
         *
         * @return A class file locator that queries the system class loader.
         */
        public static ClassFileLocator ofClassPath() {
            return new ForClassLoader(ClassLoader.getSystemClassLoader());
        }

        /**
         * Creates a class file locator for a given class loader.
         *
         * @param classLoader The class loader to be used. If this class loader represents the bootstrap class
         *                    loader which is represented by the {@code null} value, this system class loader
         *                    is used instead.
         * @return A corresponding source locator.
         */
        public static ClassFileLocator of(ClassLoader classLoader) {
            return new ForClassLoader(classLoader == null
                    ? ClassLoader.getSystemClassLoader()
                    : classLoader);
        }

        /**
         * Attempts to create a binary representation of a loaded type by requesting data from its
         * {@link java.lang.ClassLoader}.
         *
         * @param type The type of interest.
         * @return The binary representation of the supplied type.
         */
        public static byte[] read(Class<?> type) {
            try {
                ClassLoader classLoader = type.getClassLoader();
                return locate(classLoader == null
                        ? ClassLoader.getSystemClassLoader()
                        : classLoader, TypeDescription.ForLoadedType.getName(type)).resolve();
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read class file for " + type, exception);
            }
        }

        /**
         * Attempts to create a binary representation of several loaded types by requesting
         * data from their respective {@link java.lang.ClassLoader}s.
         *
         * @param type The types of interest.
         * @return A mapping of the supplied types to their binary representation.
         */
        public static Map<Class<?>, byte[]> read(Class<?>... type) {
            return read(Arrays.asList(type));
        }

        /**
         * Attempts to create a binary representation of several loaded types by requesting
         * data from their respective {@link java.lang.ClassLoader}s.
         *
         * @param types The types of interest.
         * @return A mapping of the supplied types to their binary representation.
         */
        public static Map<Class<?>, byte[]> read(Collection<? extends Class<?>> types) {
            Map<Class<?>, byte[]> result = new HashMap<Class<?>, byte[]>();
            for (Class<?> type : types) {
                result.put(type, read(type));
            }
            return result;
        }

        /**
         * Attempts to create a binary representation of several loaded types by requesting
         * data from their respective {@link java.lang.ClassLoader}s.
         *
         * @param type The types of interest.
         * @return A mapping of the supplied types' names to their binary representation.
         */
        public static Map<String, byte[]> readToNames(Class<?>... type) {
            return readToNames(Arrays.asList(type));
        }

        /**
         * Attempts to create a binary representation of several loaded types by requesting
         * data from their respective {@link java.lang.ClassLoader}s.
         *
         * @param types The types of interest.
         * @return A mapping of the supplied types' names to their binary representation.
         */
        public static Map<String, byte[]> readToNames(Collection<? extends Class<?>> types) {
            Map<String, byte[]> result = new HashMap<String, byte[]>();
            for (Class<?> type : types) {
                result.put(type.getName(), read(type));
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            return locate(classLoader, name);
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }

        /**
         * Locates the class file for the supplied type by requesting a resource from the class loader.
         *
         * @param classLoader The class loader to query for the resource.
         * @param name        The name of the type for which to locate a class file.
         * @return A resolution for the class file.
         * @throws IOException If reading the class file causes an exception.
         */
        protected static Resolution locate(ClassLoader classLoader, String name) throws IOException {
            InputStream inputStream = classLoader.getResourceAsStream(name.replace('.', '/') + CLASS_FILE_EXTENSION);
            if (inputStream != null) {
                try {
                    return new Resolution.Explicit(StreamDrainer.DEFAULT.drain(inputStream));
                } finally {
                    inputStream.close();
                }
            } else {
                return new Resolution.Illegal(name);
            }
        }

        /**
         * <p>
         * A class file locator that queries a class loader for binary representations of class files.
         * The class loader is only weakly referenced.
         * </p>
         * <p>
         * <b>Important</b>: Even when calling {@link Closeable#close()} on this class file locator, no underlying
         * class loader is closed if it implements the {@link Closeable} interface as this is typically not intended.
         * </p>
         */
        public static class WeaklyReferenced extends WeakReference<ClassLoader> implements ClassFileLocator {

            /**
             * The represented class loader's hash code.
             */
            private final int hashCode;

            /**
             * Creates a class file locator for a class loader that is weakly referenced.
             *
             * @param classLoader The class loader to represent.
             */
            protected WeaklyReferenced(ClassLoader classLoader) {
                super(classLoader);
                hashCode = System.identityHashCode(classLoader);
            }

            /**
             * Creates a class file locator for a given class loader. If the class loader is not the bootstrap
             * class loader or the system class loader which cannot be collected, the class loader is only weakly
             * referenced.
             *
             * @param classLoader The class loader to be used. If this class loader represents the bootstrap class
             *                    loader which is represented by the {@code null} value, this system class loader
             *                    is used instead.
             * @return A corresponding source locator.
             */
            public static ClassFileLocator of(ClassLoader classLoader) {
                return classLoader == null || classLoader == ClassLoader.getSystemClassLoader() || classLoader == ClassLoader.getSystemClassLoader().getParent()
                        ? ForClassLoader.of(classLoader)
                        : new WeaklyReferenced(classLoader);
            }

            /**
             * {@inheritDoc}
             */
            public Resolution locate(String name) throws IOException {
                ClassLoader classLoader = get();
                return classLoader == null
                        ? new Resolution.Illegal(name)
                        : ForClassLoader.locate(classLoader, name);
            }

            /**
             * {@inheritDoc}
             */
            public void close() {
                /* do nothing */
            }

            @Override
            public int hashCode() {
                return hashCode;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                } else if (other == null || getClass() != other.getClass()) {
                    return false;
                }
                WeaklyReferenced weaklyReferenced = (WeaklyReferenced) other;
                ClassLoader classLoader = weaklyReferenced.get();
                return classLoader != null && get() == classLoader;
            }
        }
    }

    /**
     * <p>
     * A class file locator that locates class files by querying a Java module's {@code getResourceAsStream} method.
     * </p>
     * <p>
     * <b>Important</b>: Even when calling {@link Closeable#close()} on this class file locator, no underlying
     * class loader is closed if it implements the {@link Closeable} interface as this is typically not intended.
     * </p>
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForModule implements ClassFileLocator {

        /**
         * An empty array that can be used to indicate no arguments to avoid an allocation on a reflective call.
         */
        private static final Object[] NO_ARGUMENTS = new Object[0];

        /**
         * The represented Java module.
         */
        private final JavaModule module;

        /**
         * Creates a new class file locator for a Java module.
         *
         * @param module The represented Java module.
         */
        protected ForModule(JavaModule module) {
            this.module = module;
        }

        /**
         * Returns a class file locator that exposes all class files of the boot module layer. This class file locator is only available
         * on virtual machines of version 9 or later. On earlier versions, the returned class file locator does not locate any resources.
         *
         * @return A class file locator that locates classes of the boot layer.
         */
        @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should always be wrapped for clarity")
        public static ClassFileLocator ofBootLayer() {
            try {
                Map<String, ClassFileLocator> bootModules = new HashMap<String, ClassFileLocator>();
                Class<?> layerType = Class.forName("java.lang.ModuleLayer");
                Method getPackages = JavaType.MODULE.load().getMethod("getPackages");
                for (Object rawModule : (Set<?>) layerType.getMethod("modules").invoke(layerType.getMethod("boot").invoke(null))) {
                    ClassFileLocator classFileLocator = ForModule.of(JavaModule.of(rawModule));
                    for (Object packageName : (Set<?>) getPackages.invoke(rawModule, NO_ARGUMENTS)) {
                        bootModules.put((String) packageName, classFileLocator);
                    }
                }
                return new PackageDiscriminating(bootModules);
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot process boot layer", exception);
            }
        }

        /**
         * Returns a class file locator for the provided module. If the provided module is not named, class files are located via this
         * unnamed module's class loader.
         *
         * @param module The module to create a class file locator for.
         * @return An appropriate class file locator.
         */
        public static ClassFileLocator of(JavaModule module) {
            return module.isNamed()
                    ? new ForModule(module)
                    : ForClassLoader.of(module.getClassLoader());
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            return locate(module, name);
        }

        /**
         * Creates a resolution for a Java module's class files.
         *
         * @param module   The Java module to query.
         * @param typeName The name of the type being queried.
         * @return A resolution for the query.
         * @throws IOException If an I/O exception was thrown.
         */
        protected static Resolution locate(JavaModule module, String typeName) throws IOException {
            InputStream inputStream = module.getResourceAsStream(typeName.replace('.', '/') + CLASS_FILE_EXTENSION);
            if (inputStream != null) {
                try {
                    return new Resolution.Explicit(StreamDrainer.DEFAULT.drain(inputStream));
                } finally {
                    inputStream.close();
                }
            } else {
                return new Resolution.Illegal(typeName);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }

        /**
         * <p>
         * A class file locator for a Java module that only references this module weakly. If a module was garbage collected,
         * this class file locator only returns unresolved resolutions.
         * </p>
         * <p>
         * <b>Important</b>: Even when calling {@link Closeable#close()} on this class file locator, no underlying
         * class loader is closed if it implements the {@link Closeable} interface as this is typically not intended.
         * </p>
         */
        public static class WeaklyReferenced extends WeakReference<Object> implements ClassFileLocator {

            /**
             * The represented module's hash code.
             */
            private final int hashCode;

            /**
             * Creates a class file locator for a Java module that is weakly referenced.
             *
             * @param module The raw Java module to represent.
             */
            protected WeaklyReferenced(Object module) {
                super(module);
                hashCode = System.identityHashCode(module);
            }

            /**
             * Creates a class file locator for a Java module where the module is referenced weakly. If the module is not named, the module's class loader
             * is represented instead. Module's of the boot layer are not referenced weakly.
             *
             * @param module The Java module to represent.
             * @return A suitable class file locator.
             */
            public static ClassFileLocator of(JavaModule module) {
                if (module.isNamed()) {
                    return module.getClassLoader() == null || module.getClassLoader() == ClassLoader.getSystemClassLoader() || module.getClassLoader() == ClassLoader.getSystemClassLoader().getParent()
                            ? new ForModule(module)
                            : new WeaklyReferenced(module.unwrap());
                } else {
                    return ForClassLoader.WeaklyReferenced.of(module.getClassLoader());
                }
            }

            /**
             * {@inheritDoc}
             */
            public Resolution locate(String name) throws IOException {
                Object module = get();
                return module == null
                        ? new Resolution.Illegal(name)
                        : ForModule.locate(JavaModule.of(module), name);
            }

            /**
             * {@inheritDoc}
             */
            public void close() {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public int hashCode() {
                return hashCode;
            }

            /**
             * {@inheritDoc}
             */
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                } else if (other == null || getClass() != other.getClass()) {
                    return false;
                }
                WeaklyReferenced weaklyReferenced = (WeaklyReferenced) other;
                Object module = weaklyReferenced.get();
                return module != null && get() == module;
            }
        }
    }

    /**
     * A class file locator that locates classes within a Java <i>jar</i> file.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForJarFile implements ClassFileLocator {

        /**
         * A list of potential locations of the runtime jar for different platforms.
         */
        private static final List<String> RUNTIME_LOCATIONS = Arrays.asList("lib/rt.jar", "../lib/rt.jar", "../Classes/classes.jar");

        /**
         * The jar file to read from.
         */
        private final JarFile jarFile;

        /**
         * Creates a new class file locator for the given jar file.
         *
         * @param jarFile The jar file to read from.
         */
        public ForJarFile(JarFile jarFile) {
            this.jarFile = jarFile;
        }

        /**
         * Creates a new class file locator for the given jar file.
         *
         * @param file The jar file to read from.
         * @return A class file locator for the jar file.
         * @throws IOException If an I/O exception is thrown.
         */
        public static ClassFileLocator of(File file) throws IOException {
            return new ForJarFile(new JarFile(file));
        }

        /**
         * Resolves a class file locator for the class path that reads class files directly from the file system. The resulting
         * class file locator does not imply classes on the boot path.
         *
         * @return A class file locator for the class path.
         * @throws IOException If an I/O exception occurs.
         */
        public static ClassFileLocator ofClassPath() throws IOException {
            return ofClassPath(System.getProperty("java.class.path"));
        }

        /**
         * <p>
         * Resolves a class file locator for the class path that reads class files directly from the file system.
         * </p>
         * <p>
         * <b>Note</b>: The resulting class file locator does not include classes of the bootstrap class loader.
         * </p>
         *
         * @param classPath The class path to scan with the elements separated by {@code path.separator}.
         * @return A class file locator for the class path.
         * @throws IOException If an I/O exception occurs.
         */
        public static ClassFileLocator ofClassPath(String classPath) throws IOException {
            List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>();
            for (String element : Pattern.compile(System.getProperty("path.separator"), Pattern.LITERAL).split(classPath)) {
                File file = new File(element);
                if (file.isDirectory()) {
                    classFileLocators.add(new ForFolder(file));
                } else if (file.isFile()) {
                    classFileLocators.add(of(file));
                }
            }
            return new Compound(classFileLocators);
        }

        /**
         * Resolves a class file locator for the runtime jar. If such a file does not exist or cannot be located, a runtime exception is thrown.
         *
         * @return A class file locator for the runtime jar, if available.
         * @throws IOException If an I/O exception occurs.
         */
        public static ClassFileLocator ofRuntimeJar() throws IOException {
            String javaHome = System.getProperty("java.home").replace('\\', '/');
            File runtimeJar = null;
            for (String location : RUNTIME_LOCATIONS) {
                File candidate = new File(javaHome, location);
                if (candidate.isFile()) {
                    runtimeJar = candidate;
                    break;
                }
            }
            if (runtimeJar == null) {
                throw new IllegalStateException("Runtime jar does not exist in " + javaHome + " for any of " + RUNTIME_LOCATIONS);
            }
            return of(runtimeJar);
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            ZipEntry zipEntry = jarFile.getEntry(name.replace('.', '/') + CLASS_FILE_EXTENSION);
            if (zipEntry == null) {
                return new Resolution.Illegal(name);
            } else {
                InputStream inputStream = jarFile.getInputStream(zipEntry);
                try {
                    return new Resolution.Explicit(StreamDrainer.DEFAULT.drain(inputStream));
                } finally {
                    inputStream.close();
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            jarFile.close();
        }
    }

    /**
     * A class file locator that locates classes within a Java <i>jmod</i> file. This class file locator should not be used
     * for reading modular jar files for which {@link ForJarFile} is appropriate.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForModuleFile implements ClassFileLocator {

        /**
         * The file extension of a modular Java package.
         */
        private static final String JMOD_FILE_EXTENSION = ".jmod";

        /**
         * A list of potential locations of the boot path for different platforms.
         */
        private static final List<String> BOOT_LOCATIONS = Arrays.asList("jmods", "../jmods");

        /**
         * The represented jmod file.
         */
        private final ZipFile zipFile;

        /**
         * Creates a new class file locator for a jmod file.
         *
         * @param zipFile The represented jmod file.
         */
        public ForModuleFile(ZipFile zipFile) {
            this.zipFile = zipFile;
        }

        /**
         * Creates a new class file locator for this VM's boot module path.
         *
         * @return A class file locator for this VM's boot module path.
         * @throws IOException If an I/O error occurs.
         */
        public static ClassFileLocator ofBootPath() throws IOException {
            String javaHome = System.getProperty("java.home").replace('\\', '/');
            File bootPath = null;
            for (String location : BOOT_LOCATIONS) {
                File candidate = new File(javaHome, location);
                if (candidate.isDirectory()) {
                    bootPath = candidate;
                    break;
                }
            }
            if (bootPath == null) {
                throw new IllegalStateException("Boot modules do not exist in " + javaHome + " for any of " + BOOT_LOCATIONS);
            }
            return ofBootPath(bootPath);
        }

        /**
         * Creates a new class file locator for a Java boot module path.
         *
         * @param bootPath The boot path folder.
         * @return A class file locator for this VMs boot module path.
         * @throws IOException If an I/O error occurs.
         */
        public static ClassFileLocator ofBootPath(File bootPath) throws IOException {
            File[] module = bootPath.listFiles();
            if (module == null) {
                return NoOp.INSTANCE;
            }
            List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>(module.length);
            for (File aModule : module) {
                if (aModule.isFile()) {
                    classFileLocators.add(of(aModule));
                }
            }
            return new Compound(classFileLocators);
        }

        /**
         * <p>
         * Resolves a class file locator for this VM's Java module path that reads class files directly from the file system.
         * </p>
         * <p>
         * <b>Note</b>: The resulting class file locator does not include classes of the bootstrap class loader.
         * </p>
         *
         * @return A class file locator for the class path.
         * @throws IOException If an I/O exception occurs.
         */
        public static ClassFileLocator ofModulePath() throws IOException {
            String modulePath = System.getProperty("jdk.module.path");
            return modulePath == null
                    ? NoOp.INSTANCE
                    : ofModulePath(modulePath);
        }

        /**
         * <p>
         * Resolves a class file locator for a Java module path that reads class files directly from the file system. All
         * elements of the module path are resolved relative to this VM's {@code user.dir}.
         * </p>
         * <p>
         * <b>Note</b>: The resulting class file locator does not include classes of the bootstrap class loader.
         * </p>
         *
         * @param modulePath The module path to scan with the elements separated by {@code path.separator}.
         * @return A class file locator for the class path.
         * @throws IOException If an I/O exception occurs.
         */
        public static ClassFileLocator ofModulePath(String modulePath) throws IOException {
            return ofModulePath(modulePath, System.getProperty("user.dir"));
        }

        /**
         * <p>
         * Resolves a class file locator for a Java module path that reads class files directly from the file system.
         * </p>
         * <p>
         * <b>Note</b>: The resulting class file locator does not include classes of the bootstrap class loader.
         * </p>
         *
         * @param modulePath The module path to scan with the elements separated by {@code path.separator}.
         * @param baseFolder The relative location of the elements on the module path.
         * @return A class file locator for the class path.
         * @throws IOException If an I/O exception occurs.
         */
        public static ClassFileLocator ofModulePath(String modulePath, String baseFolder) throws IOException {
            List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>();
            for (String element : Pattern.compile(System.getProperty("path.separator"), Pattern.LITERAL).split(modulePath)) {
                File file = new File(baseFolder, element);
                if (file.isDirectory()) {
                    File[] module = file.listFiles();
                    if (module != null) {
                        for (File aModule : module) {
                            if (aModule.isDirectory()) {
                                classFileLocators.add(new ForFolder(aModule));
                            } else if (aModule.isFile()) {
                                classFileLocators.add(aModule.getName().endsWith(JMOD_FILE_EXTENSION)
                                        ? of(aModule)
                                        : ForJarFile.of(aModule));
                            }
                        }
                    }
                } else if (file.isFile()) {
                    classFileLocators.add(file.getName().endsWith(JMOD_FILE_EXTENSION)
                            ? of(file)
                            : ForJarFile.of(file));
                }
            }
            return new Compound(classFileLocators);
        }

        /**
         * Returns a class file locator for the given module file.
         *
         * @param file The module file.
         * @return A class file locator for the given module
         * @throws IOException If an I/O error occurs.
         */
        public static ClassFileLocator of(File file) throws IOException {
            return new ForModuleFile(new ZipFile(file));
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            ZipEntry zipEntry = zipFile.getEntry("classes/" + name.replace('.', '/') + CLASS_FILE_EXTENSION);
            if (zipEntry == null) {
                return new Resolution.Illegal(name);
            } else {
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                try {
                    return new Resolution.Explicit(StreamDrainer.DEFAULT.drain(inputStream));
                } finally {
                    inputStream.close();
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            zipFile.close();
        }
    }

    /**
     * A class file locator that finds files from a standardized Java folder structure with
     * folders donating packages and class files being saved as {@code <classname>.class} files
     * within their package folder.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForFolder implements ClassFileLocator {

        /**
         * The base folder of the package structure.
         */
        private final File folder;

        /**
         * Creates a new class file locator for a folder structure of class files.
         *
         * @param folder The base folder of the package structure.
         */
        public ForFolder(File folder) {
            this.folder = folder;
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            File file = new File(folder, name.replace('.', File.separatorChar) + CLASS_FILE_EXTENSION);
            if (file.exists()) {
                InputStream inputStream = new FileInputStream(file);
                try {
                    return new Resolution.Explicit(StreamDrainer.DEFAULT.drain(inputStream));
                } finally {
                    inputStream.close();
                }
            } else {
                return new Resolution.Illegal(name);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }
    }

    /**
     * A class file locator that reads class files from one or several URLs. The reading is accomplished via using an {@link URLClassLoader}.
     * Doing so, boot loader resources might be located additionally to those found via the specified URLs.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForUrl implements ClassFileLocator {

        /**
         * The class loader that delegates to the URLs.
         */
        private final ClassLoader classLoader;

        /**
         * Creates a new class file locator for the given URLs.
         *
         * @param url The URLs to search for class files.
         */
        public ForUrl(URL... url) {
            classLoader = AccessController.doPrivileged(new ClassLoaderCreationAction(url));
        }

        /**
         * Creates a new class file locator for the given URLs.
         *
         * @param urls The URLs to search for class files.
         */
        public ForUrl(Collection<? extends URL> urls) {
            this(urls.toArray(new URL[urls.size()]));
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            return ForClassLoader.locate(classLoader, name);
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            if (classLoader instanceof Closeable) {
                ((Closeable) classLoader).close();
            }
        }

        /**
         * An action to create a class loader with the purpose of locating classes from an URL location.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class ClassLoaderCreationAction implements PrivilegedAction<ClassLoader> {

            /**
             * The URLs to locate classes from.
             */
            private final URL[] url;

            /**
             * Creates a new class loader creation action.
             *
             * @param url The URLs to locate classes from.
             */
            protected ClassLoaderCreationAction(URL[] url) {
                this.url = url;
            }

            /**
             * {@inheritDoc}
             */
            public ClassLoader run() {
                return new URLClassLoader(url, ClassLoadingStrategy.BOOTSTRAP_LOADER);
            }
        }
    }

    /**
     * A Java agent that allows the location of class files by emulating a retransformation. Note that this class file
     * locator causes a class to be loaded in order to look up its class file. Also, this locator does deliberately not
     * support the look-up of classes that represent lambda expressions.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class AgentBased implements ClassFileLocator {

        /**
         * The name of the Byte Buddy {@code net.bytebuddy.agent.Installer} class.
         */
        private static final String INSTALLER_TYPE = "net.bytebuddy.agent.Installer";

        /**
         * The name of the {@code net.bytebuddy.agent.Installer} getter for reading an installed {@link Instrumentation}.
         */
        private static final String INSTRUMENTATION_GETTER = "getInstrumentation";

        /**
         * Indicator for access to a static member via reflection to make the code more readable.
         */
        private static final Object STATIC_MEMBER = null;

        /**
         * A dispatcher for interacting with the instrumentation API.
         */
        private static final Dispatcher DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

        /**
         * The instrumentation instance to use for looking up the binary format of a type.
         */
        private final Instrumentation instrumentation;

        /**
         * The delegate to load a class by its name.
         */
        private final ClassLoadingDelegate classLoadingDelegate;

        /**
         * Creates an agent-based class file locator.
         *
         * @param instrumentation The instrumentation to be used.
         * @param classLoader     The class loader to read a class from.
         */
        public AgentBased(Instrumentation instrumentation, ClassLoader classLoader) {
            this(instrumentation, ClassLoadingDelegate.Default.of(classLoader));
        }

        /**
         * Creates an agent-based class file locator.
         *
         * @param instrumentation      The instrumentation to be used.
         * @param classLoadingDelegate The delegate responsible for class loading.
         */
        public AgentBased(Instrumentation instrumentation, ClassLoadingDelegate classLoadingDelegate) {
            if (!DISPATCHER.isRetransformClassesSupported(instrumentation)) {
                throw new IllegalArgumentException(instrumentation + " does not support retransformation");
            }
            this.instrumentation = instrumentation;
            this.classLoadingDelegate = classLoadingDelegate;
        }

        /**
         * Returns an agent-based class file locator for the given class loader and an already installed
         * Byte Buddy-agent.
         *
         * @param classLoader The class loader that is expected to load the looked-up a class.
         * @return A class file locator for the given class loader based on a Byte Buddy agent.
         */
        public static ClassFileLocator fromInstalledAgent(ClassLoader classLoader) {
            try {
                return new AgentBased((Instrumentation) ClassLoader.getSystemClassLoader()
                        .loadClass(INSTALLER_TYPE)
                        .getMethod(INSTRUMENTATION_GETTER)
                        .invoke(STATIC_MEMBER), classLoader);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", exception);
            }
        }

        /**
         * Returns a class file locator that is capable of locating a class file for the given type using the given instrumentation instance.
         *
         * @param instrumentation The instrumentation instance to query for a retransformation.
         * @param type            The locatable type which class loader is used as a fallback.
         * @return A class file locator for locating the class file of the given type.
         */
        public static ClassFileLocator of(Instrumentation instrumentation, Class<?> type) {
            return new AgentBased(instrumentation, ClassLoadingDelegate.Explicit.of(type));
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) {
            try {
                ExtractionClassFileTransformer classFileTransformer = new ExtractionClassFileTransformer(classLoadingDelegate.getClassLoader(), name);
                DISPATCHER.addTransformer(instrumentation, classFileTransformer, true);
                try {
                    DISPATCHER.retransformClasses(instrumentation, new Class<?>[]{classLoadingDelegate.locate(name)});
                    byte[] binaryRepresentation = classFileTransformer.getBinaryRepresentation();
                    return binaryRepresentation == null
                            ? new Resolution.Illegal(name)
                            : new Resolution.Explicit(binaryRepresentation);
                } finally {
                    instrumentation.removeTransformer(classFileTransformer);
                }
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception ignored) {
                return new Resolution.Illegal(name);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }

        /**
         * A dispatcher to interact with the {@link Instrumentation} API.
         */
        protected interface Dispatcher {

            /**
             * Invokes the {@code Instrumentation#isRetransformClassesSupported} method.
             *
             * @param instrumentation The instrumentation instance to invoke the method on.
             * @return {@code true} if the supplied instrumentation instance supports retransformation.
             */
            boolean isRetransformClassesSupported(Instrumentation instrumentation);

            /**
             * Registers a transformer.
             *
             * @param instrumentation      The instrumentation instance to invoke the method on.
             * @param classFileTransformer The class file transformer to register.
             * @param canRetransform       {@code true} if the class file transformer should be invoked upon a retransformation.
             */
            void addTransformer(Instrumentation instrumentation, ClassFileTransformer classFileTransformer, boolean canRetransform);

            /**
             * Retransforms the supplied classes.
             *
             * @param instrumentation The instrumentation instance to invoke the method on.
             * @param type            The types to retransform.
             * @throws UnmodifiableClassException If any of the supplied types are unmodifiable.
             */
            void retransformClasses(Instrumentation instrumentation, Class<?>[] type) throws UnmodifiableClassException;

            /**
             * An action to create a {@link Dispatcher}.
             */
            enum CreationAction implements PrivilegedAction<Dispatcher> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher run() {
                    try {
                        return new ForJava6CapableVm(Instrumentation.class.getMethod("isRetransformClassesSupported"),
                                Instrumentation.class.getMethod("addTransformer", ClassFileTransformer.class, boolean.class),
                                Instrumentation.class.getMethod("retransformClasses", Class[].class));
                    } catch (NoSuchMethodException ignored) {
                        return ForLegacyVm.INSTANCE;
                    }
                }
            }

            /**
             * A dispatcher for a VM that does not support retransformation.
             */
            enum ForLegacyVm implements Dispatcher {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public boolean isRetransformClassesSupported(Instrumentation instrumentation) {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public void addTransformer(Instrumentation instrumentation, ClassFileTransformer classFileTransformer, boolean canRetransform) {
                    throw new IllegalStateException("The current VM does not support class retransformation");
                }

                /**
                 * {@inheritDoc}
                 */
                public void retransformClasses(Instrumentation instrumentation, Class<?>[] type) {
                    throw new IllegalStateException("The current VM does not support class retransformation");
                }
            }

            /**
             * A dispatcher for a Java 6 capable VM.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForJava6CapableVm implements Dispatcher {

                /**
                 * The {@code Instrumentation#isRetransformClassesSupported} method.
                 */
                private final Method isRetransformClassesSupported;

                /**
                 * The {@code Instrumentation#addTransformer} method.
                 */
                private final Method addTransformer;

                /**
                 * The {@code Instrumentation#retransformClasses} method.
                 */
                private final Method retransformClasses;

                /**
                 * Creates a dispatcher for a Java 6 capable VM.
                 *
                 * @param isRetransformClassesSupported The {@code Instrumentation#isRetransformClassesSupported} method.
                 * @param addTransformer                The {@code Instrumentation#addTransformer} method.
                 * @param retransformClasses            The {@code Instrumentation#retransformClasses} method.
                 */
                protected ForJava6CapableVm(Method isRetransformClassesSupported, Method addTransformer, Method retransformClasses) {
                    this.isRetransformClassesSupported = isRetransformClassesSupported;
                    this.addTransformer = addTransformer;
                    this.retransformClasses = retransformClasses;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isRetransformClassesSupported(Instrumentation instrumentation) {
                    try {
                        return (Boolean) isRetransformClassesSupported.invoke(instrumentation);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.instrument.Instrumentation#isRetransformClassesSupported", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.instrument.Instrumentation#isRetransformClassesSupported", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void addTransformer(Instrumentation instrumentation, ClassFileTransformer classFileTransformer, boolean canRetransform) {
                    try {
                        addTransformer.invoke(instrumentation, classFileTransformer, canRetransform);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.instrument.Instrumentation#addTransformer", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.instrument.Instrumentation#addTransformer", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void retransformClasses(Instrumentation instrumentation, Class<?>[] type) throws UnmodifiableClassException {
                    try {
                        retransformClasses.invoke(instrumentation, (Object) type);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.instrument.Instrumentation#retransformClasses", exception);
                    } catch (InvocationTargetException exception) {
                        Throwable cause = exception.getCause();
                        if (cause instanceof UnmodifiableClassException) {
                            throw (UnmodifiableClassException) cause;
                        } else {
                            throw new IllegalStateException("Error invoking java.lang.instrument.Instrumentation#retransformClasses", cause);
                        }
                    }
                }
            }
        }

        /**
         * A delegate that is queried for loading a class.
         */
        public interface ClassLoadingDelegate {

            /**
             * Loads a class by its name.
             *
             * @param name The name of the type.
             * @return The class with the given name.
             * @throws ClassNotFoundException If a class cannot be found.
             */
            Class<?> locate(String name) throws ClassNotFoundException;

            /**
             * Returns the underlying class loader.
             *
             * @return The underlying class loader.
             */
            ClassLoader getClassLoader();

            /**
             * A default implementation of a class loading delegate.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Default implements ClassLoadingDelegate {

                /**
                 * The underlying class loader.
                 */
                protected final ClassLoader classLoader;

                /**
                 * Creates a default class loading delegate.
                 *
                 * @param classLoader The class loader to be queried.
                 */
                protected Default(ClassLoader classLoader) {
                    this.classLoader = classLoader;
                }

                /**
                 * Creates a class loading delegate for the given class loader.
                 *
                 * @param classLoader The class loader for which to create a delegate.
                 * @return The class loading delegate for the provided class loader.
                 */
                public static ClassLoadingDelegate of(ClassLoader classLoader) {
                    return ForDelegatingClassLoader.isDelegating(classLoader)
                            ? new ForDelegatingClassLoader(classLoader)
                            : new Default(classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader);
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> locate(String name) throws ClassNotFoundException {
                    return classLoader.loadClass(name);
                }

                /**
                 * {@inheritDoc}
                 */
                public ClassLoader getClassLoader() {
                    return classLoader;
                }
            }

            /**
             * A class loading delegate that accounts for a {@code sun.reflect.DelegatingClassLoader} which
             * cannot load its own classes by name.
             */
            class ForDelegatingClassLoader extends Default {

                /**
                 * The name of the delegating class loader.
                 */
                private static final String DELEGATING_CLASS_LOADER_NAME = "sun.reflect.DelegatingClassLoader";

                /**
                 * An index indicating the first element of a collection.
                 */
                private static final int ONLY = 0;

                /**
                 * A dispatcher for extracting a class loader's loaded classes.
                 */
                private static final Dispatcher.Initializable DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

                /**
                 * Creates a class loading delegate for a delegating class loader.
                 *
                 * @param classLoader The delegating class loader.
                 */
                protected ForDelegatingClassLoader(ClassLoader classLoader) {
                    super(classLoader);
                }

                /**
                 * Checks if a class loader is a delegating class loader.
                 *
                 * @param classLoader The class loader to inspect.
                 * @return {@code true} if the class loader is a delegating class loader.
                 */
                protected static boolean isDelegating(ClassLoader classLoader) {
                    return classLoader != null && classLoader.getClass().getName().equals(DELEGATING_CLASS_LOADER_NAME);
                }

                /**
                 * {@inheritDoc}
                 */
                @SuppressWarnings("unchecked")
                public Class<?> locate(String name) throws ClassNotFoundException {
                    Vector<Class<?>> classes;
                    try {
                        classes = DISPATCHER.initialize().extract(classLoader);
                    } catch (RuntimeException ignored) {
                        return super.locate(name);
                    }
                    if (classes.size() != 1) {
                        return super.locate(name);
                    }
                    Class<?> type = classes.get(ONLY);
                    return TypeDescription.ForLoadedType.getName(type).equals(name)
                            ? type
                            : super.locate(name);
                }

                /**
                 * Representation of a Java {@link java.lang.reflect.Field}.
                 */
                protected interface Dispatcher {

                    /**
                     * Reads the classes of the represented collection.
                     *
                     * @param classLoader The class loader to read from.
                     * @return The class loader's loaded classes.
                     */
                    Vector<Class<?>> extract(ClassLoader classLoader);

                    /**
                     * An uninitialized version of a dispatcher for extracting a class loader's loaded classes.
                     */
                    interface Initializable {

                        /**
                         * Initializes the dispatcher.
                         *
                         * @return An initialized dispatcher.
                         */
                        Dispatcher initialize();
                    }

                    /**
                     * An action for creating a dispatcher.
                     */
                    enum CreationAction implements PrivilegedAction<Initializable> {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        /**
                         * {@inheritDoc}
                         */
                        public Initializable run() {
                            try {
                                return new Dispatcher.Resolved(ClassLoader.class.getDeclaredField("classes"));
                            } catch (Exception exception) {
                                return new Dispatcher.Unresolved(exception.getMessage());
                            }
                        }
                    }

                    /**
                     * Represents a field that could be located.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    class Resolved implements Dispatcher, Initializable, PrivilegedAction<Dispatcher> {

                        /**
                         * The represented field.
                         */
                        private final Field field;

                        /**
                         * Creates a new resolved field.
                         *
                         * @param field the represented field.l
                         */
                        public Resolved(Field field) {
                            this.field = field;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Dispatcher initialize() {
                            return AccessController.doPrivileged(this);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        @SuppressWarnings("unchecked")
                        public Vector<Class<?>> extract(ClassLoader classLoader) {
                            try {
                                return (Vector<Class<?>>) field.get(classLoader);
                            } catch (IllegalAccessException exception) {
                                throw new IllegalStateException("Cannot access field", exception);
                            }
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Dispatcher run() {
                            field.setAccessible(true);
                            return this;
                        }
                    }

                    /**
                     * Represents a field that could not be located.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    class Unresolved implements Initializable {

                        /**
                         * The reason why this dispatcher is unavailable.
                         */
                        private final String message;

                        /**
                         * Creates a representation of a non-resolved field.
                         *
                         * @param message The reason why this dispatcher is unavailable.
                         */
                        public Unresolved(String message) {
                            this.message = message;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Dispatcher initialize() {
                            throw new UnsupportedOperationException("Could not locate classes vector: " + message);
                        }
                    }
                }
            }

            /**
             * A class loading delegate that allows the location of explicitly registered classes that cannot
             * be located by a class loader directly. This allows for locating classes that are loaded by
             * an anonymous class loader which does not register its classes in a system dictionary.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Explicit implements ClassLoadingDelegate {

                /**
                 * A class loading delegate that is queried for classes that are not registered explicitly.
                 */
                private final ClassLoadingDelegate fallbackDelegate;

                /**
                 * The map of registered classes mapped by their name.
                 */
                private final Map<String, Class<?>> types;

                /**
                 * Creates a new class loading delegate with a possibility of looking up explicitly
                 * registered classes.
                 *
                 * @param classLoader The class loader to be used for looking up classes.
                 * @param types       A collection of classes that cannot be looked up explicitly.
                 */
                public Explicit(ClassLoader classLoader, Collection<? extends Class<?>> types) {
                    this(Default.of(classLoader), types);
                }

                /**
                 * Creates a new class loading delegate with a possibility of looking up explicitly
                 * registered classes.
                 *
                 * @param fallbackDelegate The class loading delegate to query for any class that is not
                 *                         registered explicitly.
                 * @param types            A collection of classes that cannot be looked up explicitly.
                 */
                public Explicit(ClassLoadingDelegate fallbackDelegate, Collection<? extends Class<?>> types) {
                    this.fallbackDelegate = fallbackDelegate;
                    this.types = new HashMap<String, Class<?>>();
                    for (Class<?> type : types) {
                        this.types.put(TypeDescription.ForLoadedType.getName(type), type);
                    }
                }

                /**
                 * Creates an explicit class loading delegate for the given type.
                 *
                 * @param type The type that is explicitly locatable.
                 * @return A suitable class loading delegate.
                 */
                public static ClassLoadingDelegate of(Class<?> type) {
                    return new Explicit(type.getClassLoader(), Collections.singleton(type));
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> locate(String name) throws ClassNotFoundException {
                    Class<?> type = types.get(name);
                    return type == null
                            ? fallbackDelegate.locate(name)
                            : type;
                }

                /**
                 * {@inheritDoc}
                 */
                public ClassLoader getClassLoader() {
                    return fallbackDelegate.getClassLoader();
                }
            }
        }

        /**
         * A non-operational class file transformer that remembers the binary format of a given class.
         */
        protected static class ExtractionClassFileTransformer implements ClassFileTransformer {

            /**
             * An indicator that an attempted class file transformation did not alter the handed class file.
             */
            private static final byte[] DO_NOT_TRANSFORM = null;

            /**
             * The class loader that is expected to have loaded the looked-up a class.
             */
            private final ClassLoader classLoader;

            /**
             * The name of the type to look up.
             */
            private final String typeName;

            /**
             * The binary representation of the looked-up class.
             */
            @SuppressFBWarnings(value = "VO_VOLATILE_REFERENCE_TO_ARRAY", justification = "The array is not to be modified by contract")
            private volatile byte[] binaryRepresentation;

            /**
             * Creates a class file transformer for the purpose of extraction.
             *
             * @param classLoader The class loader that is expected to have loaded the looked-up a class.
             * @param typeName    The name of the type to look up.
             */
            protected ExtractionClassFileTransformer(ClassLoader classLoader, String typeName) {
                this.classLoader = classLoader;
                this.typeName = typeName;
            }

            /**
             * {@inheritDoc}
             */
            @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "The array is not to be modified by contract")
            public byte[] transform(ClassLoader classLoader,
                                    String internalName,
                                    Class<?> redefinedType,
                                    ProtectionDomain protectionDomain,
                                    byte[] binaryRepresentation) {
                if (internalName != null && isChildOf(this.classLoader).matches(classLoader) && typeName.equals(internalName.replace('/', '.'))) {
                    this.binaryRepresentation = binaryRepresentation.clone();
                }
                return DO_NOT_TRANSFORM;
            }

            /**
             * Returns the binary representation of the class file that was looked up. The returned array must never be modified.
             *
             * @return The binary representation of the class file or {@code null} if no such class file could
             * be located.
             */
            @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The array is not to be modified by contract")
            protected byte[] getBinaryRepresentation() {
                return binaryRepresentation;
            }
        }
    }

    /**
     * A class file locator that discriminates by a type's package.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class PackageDiscriminating implements ClassFileLocator {

        /**
         * A mapping of package names to class file locators.
         */
        private final Map<String, ClassFileLocator> classFileLocators;

        /**
         * Creates a new package-discriminating class file locator.
         *
         * @param classFileLocators A mapping of package names to class file locators where an empty string donates the default package.
         */
        public PackageDiscriminating(Map<String, ClassFileLocator> classFileLocators) {
            this.classFileLocators = classFileLocators;
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            int packageIndex = name.lastIndexOf('.');
            ClassFileLocator classFileLocator = classFileLocators.get(packageIndex == -1
                    ? NamedElement.EMPTY_NAME
                    : name.substring(0, packageIndex));
            return classFileLocator == null
                    ? new Resolution.Illegal(name)
                    : classFileLocator.locate(name);
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            for (ClassFileLocator classFileLocator : classFileLocators.values()) {
                classFileLocator.close();
            }
        }
    }

    /**
     * A compound {@link ClassFileLocator} that chains several locators.
     * Any class file locator is queried in the supplied order until one locator is able to provide an input
     * stream of the class file.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Compound implements ClassFileLocator, Closeable {

        /**
         * The {@link ClassFileLocator}s which are represented by this compound
         * class file locator  in the order of their application.
         */
        private final List<ClassFileLocator> classFileLocators;

        /**
         * Creates a new compound class file locator.
         *
         * @param classFileLocator The {@link ClassFileLocator}s to be
         *                         represented by this compound class file locator in the order of their application.
         */
        public Compound(ClassFileLocator... classFileLocator) {
            this(Arrays.asList(classFileLocator));
        }

        /**
         * Creates a new compound class file locator.
         *
         * @param classFileLocators The {@link ClassFileLocator}s to be represented by this compound class file locator in
         *                          the order of their application.
         */
        public Compound(List<? extends ClassFileLocator> classFileLocators) {
            this.classFileLocators = new ArrayList<ClassFileLocator>();
            for (ClassFileLocator classFileLocator : classFileLocators) {
                if (classFileLocator instanceof Compound) {
                    this.classFileLocators.addAll(((Compound) classFileLocator).classFileLocators);
                } else if (!(classFileLocator instanceof NoOp)) {
                    this.classFileLocators.add(classFileLocator);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            for (ClassFileLocator classFileLocator : classFileLocators) {
                Resolution resolution = classFileLocator.locate(name);
                if (resolution.isResolved()) {
                    return resolution;
                }
            }
            return new Resolution.Illegal(name);
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            for (ClassFileLocator classFileLocator : classFileLocators) {
                classFileLocator.close();
            }
        }
    }
}
