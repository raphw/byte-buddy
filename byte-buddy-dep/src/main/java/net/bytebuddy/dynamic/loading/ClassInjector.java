package net.bytebuddy.dynamic.loading;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.RandomString;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * <p>
 * A class injector is capable of injecting classes into a {@link java.lang.ClassLoader} without
 * requiring the class loader to being able to explicitly look up these classes.
 * </p>
 * <p>
 * <b>Important</b>: Byte Buddy does not supply privileges when injecting code. When using a {@link SecurityManager},
 * the user of this injector is responsible for providing access to non-public properties.
 * </p>
 */
public interface ClassInjector {

    /**
     * A convenience reference to the default protection domain which is {@code null}.
     */
    ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

    /**
     * Determines the default behavior for type injections when a type is already loaded.
     */
    boolean DEFAULT_FORBID_EXISTING = false;

    /**
     * Injects the given types into the represented class loader.
     *
     * @param types The types to load via injection.
     * @return The loaded types that were passed as arguments.
     */
    Map<TypeDescription, Class<?>> inject(Map<? extends TypeDescription, byte[]> types);

    /**
     * A class injector that uses reflective method calls.
     */
    class UsingReflection implements ClassInjector {

        /**
         * Indicates that an array should be included from the first index position. Improves source code readabilty.
         */
        private static final int FROM_BEGINNING = 0;

        /**
         * The dispatcher to use for accessing a class loader via reflection.
         */
        private static final Dispatcher.Initializable DISPATCHER = dispatcher();

        /**
         * Obtains the reflective instances used by this injector or a no-op instance that throws the exception
         * that occurred when attempting to obtain the reflective member instances.
         *
         * @return A dispatcher for the current VM.
         */
        @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
        private static Dispatcher.Initializable dispatcher() {
            try {
                return new Dispatcher.Resolved(ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class),
                        ClassLoader.class.getDeclaredMethod("defineClass",
                                String.class,
                                byte[].class,
                                int.class,
                                int.class,
                                ProtectionDomain.class),
                        ClassLoader.class.getDeclaredMethod("getPackage", String.class),
                        ClassLoader.class.getDeclaredMethod("definePackage",
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                URL.class));
            } catch (Exception exception) {
                return new Dispatcher.Faulty(exception);
            }
        }

        /**
         * The class loader into which the classes are to be injected.
         */
        private final ClassLoader classLoader;

        /**
         * The protection domain that is used when loading classes.
         */
        private final ProtectionDomain protectionDomain;

        /**
         * The package definer to be queried for package definitions.
         */
        private final PackageDefinitionStrategy packageDefinitionStrategy;

        /**
         * Determines if an exception should be thrown when attempting to load a type that already exists.
         */
        private final boolean forbidExisting;

        /**
         * Creates a new injector for the given {@link java.lang.ClassLoader} and a default {@link java.security.ProtectionDomain} and a
         * trivial {@link PackageDefinitionStrategy} which does not trigger an error when discovering existent classes.
         *
         * @param classLoader The {@link java.lang.ClassLoader} into which new class definitions are to be injected.
         */
        public UsingReflection(ClassLoader classLoader) {
            this(classLoader, DEFAULT_PROTECTION_DOMAIN);
        }

        /**
         * Creates a new injector for the given {@link java.lang.ClassLoader} and a default {@link PackageDefinitionStrategy} where the
         * injection of existent classes does not trigger an error.
         *
         * @param classLoader      The {@link java.lang.ClassLoader} into which new class definitions are to be injected.
         * @param protectionDomain The protection domain to apply during class definition.
         */
        public UsingReflection(ClassLoader classLoader, ProtectionDomain protectionDomain) {
            this(classLoader,
                    protectionDomain,
                    PackageDefinitionStrategy.Trivial.INSTANCE,
                    DEFAULT_FORBID_EXISTING);
        }

        /**
         * Creates a new injector for the given {@link java.lang.ClassLoader} and {@link java.security.ProtectionDomain}.
         *
         * @param classLoader               The {@link java.lang.ClassLoader} into which new class definitions are to be injected.
         * @param protectionDomain          The protection domain to apply during class definition.
         * @param packageDefinitionStrategy The package definer to be queried for package definitions.
         * @param forbidExisting            Determines if an exception should be thrown when attempting to load a type that already exists.
         */
        public UsingReflection(ClassLoader classLoader,
                               ProtectionDomain protectionDomain,
                               PackageDefinitionStrategy packageDefinitionStrategy,
                               boolean forbidExisting) {
            if (classLoader == null) {
                throw new IllegalArgumentException("Cannot inject classes into the bootstrap class loader");
            }
            this.classLoader = classLoader;
            this.protectionDomain = protectionDomain;
            this.packageDefinitionStrategy = packageDefinitionStrategy;
            this.forbidExisting = forbidExisting;
        }

        /**
         * Creates a class injector for the system class loader.
         *
         * @return A class injector for the system class loader.
         */
        public static ClassInjector ofSystemClassLoader() {
            return new UsingReflection(ClassLoader.getSystemClassLoader());
        }

        @Override
        public Map<TypeDescription, Class<?>> inject(Map<? extends TypeDescription, byte[]> types) {
            synchronized (classLoader) {
                Map<TypeDescription, Class<?>> loadedTypes = new HashMap<TypeDescription, Class<?>>();
                for (Map.Entry<? extends TypeDescription, byte[]> entry : types.entrySet()) {
                    String typeName = entry.getKey().getName();
                    Dispatcher dispatcher = DISPATCHER.initialize();
                    Class<?> type = dispatcher.findClass(classLoader, typeName);
                    if (type == null) {
                        int packageIndex = typeName.lastIndexOf('.');
                        if (packageIndex != -1) {
                            String packageName = typeName.substring(0, packageIndex);
                            PackageDefinitionStrategy.Definition definition = packageDefinitionStrategy.define(classLoader, packageName, typeName);
                            if (definition.isDefined()) {
                                Package definedPackage = dispatcher.getPackage(classLoader, packageName);
                                if (definedPackage == null) {
                                    dispatcher.definePackage(classLoader,
                                            packageName,
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
                        byte[] binaryRepresentation = entry.getValue();
                        type = dispatcher.loadClass(classLoader,
                                typeName,
                                binaryRepresentation,
                                FROM_BEGINNING,
                                binaryRepresentation.length,
                                protectionDomain);
                    } else if (forbidExisting) {
                        throw new IllegalStateException("Cannot inject already loaded type: " + type);
                    }
                    loadedTypes.put(entry.getKey(), type);
                }
                return loadedTypes;
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            UsingReflection that = (UsingReflection) other;
            return classLoader.equals(that.classLoader)
                    && forbidExisting == that.forbidExisting
                    && packageDefinitionStrategy.equals(that.packageDefinitionStrategy)
                    && !(protectionDomain != null ? !protectionDomain.equals(that.protectionDomain) : that.protectionDomain != null);
        }

        @Override
        public int hashCode() {
            int result = classLoader.hashCode();
            result = 31 * result + (protectionDomain != null ? protectionDomain.hashCode() : 0);
            result = 31 * result + (forbidExisting ? 1 : 0);
            result = 31 * result + packageDefinitionStrategy.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ClassInjector.UsingReflection{" +
                    "classLoader=" + classLoader +
                    ", protectionDomain=" + protectionDomain +
                    ", packageDefinitionStrategy=" + packageDefinitionStrategy +
                    ", forbidExisting=" + forbidExisting +
                    '}';
        }

        /**
         * A dispatcher for accessing a {@link ClassLoader} reflectively.
         */
        protected interface Dispatcher {

            /**
             * Looks up a class from the given class loader.
             *
             * @param classLoader The class loader for which a class should be located.
             * @param name        The binary name of the class that should be located.
             * @return The class for the binary name or {@code null} if no such class is defined for the provided class loader.
             */
            Class<?> findClass(ClassLoader classLoader, String name);

            /**
             * Defines a class for the given class loader.
             *
             * @param classLoader          The class loader for which a new class should be defined.
             * @param name                 The binary name of the class that should be defined.
             * @param binaryRepresentation The binary representation of the class.
             * @param startIndex           The start index of the provided binary representation.
             * @param endIndex             The final index of the binary representation.
             * @param protectionDomain     The protection domain for the defined class.
             * @return The defined, loaded class.
             */
            Class<?> loadClass(ClassLoader classLoader,
                               String name,
                               byte[] binaryRepresentation,
                               int startIndex,
                               int endIndex,
                               ProtectionDomain protectionDomain);

            /**
             * Looks up a package from a class loader.
             *
             * @param classLoader The class loader to query.
             * @param name        The binary name of the package.
             * @return The package for the given name as defined by the provided class loader or {@code null} if no such package exists.
             */
            Package getPackage(ClassLoader classLoader, String name);

            /**
             * Defines a package for the given class loader.
             *
             * @param classLoader           The class loader for which a package is to be defined.
             * @param packageName           The binary name of the package.
             * @param specificationTitle    The specification title of the package or {@code null} if no specification title exists.
             * @param specificationVersion  The specification version of the package or {@code null} if no specification version exists.
             * @param specificationVendor   The specification vendor of the package or {@code null} if no specification vendor exists.
             * @param implementationTitle   The implementation title of the package or {@code null} if no implementation title exists.
             * @param implementationVersion The implementation version of the package or {@code null} if no implementation version exists.
             * @param implementationVendor  The implementation vendor of the package or {@code null} if no implementation vendor exists.
             * @param sealBase              The seal base URL or {@code null} if the package should not be sealed.
             * @return The defined package.
             */
            Package definePackage(ClassLoader classLoader,
                                  String packageName,
                                  String specificationTitle,
                                  String specificationVersion,
                                  String specificationVendor,
                                  String implementationTitle,
                                  String implementationVersion,
                                  String implementationVendor,
                                  URL sealBase);

            /**
             * Initializes a dispatcher to make non-accessible APIs accessible.
             */
            interface Initializable {

                /**
                 * Initializes this dispatcher.
                 *
                 * @return The initiailized dispatcher.
                 */
                Dispatcher initialize();
            }

            /**
             * Represents a successfully loaded method lookup for the dispatcher.
             */
            class Resolved implements Dispatcher, Initializable {

                /**
                 * An accessible instance of {@link ClassLoader#findLoadedClass(String)}.
                 */
                private final Method findLoadedClass;

                /**
                 * An accessible instance of {@link ClassLoader#loadClass(String)}.
                 */
                private final Method loadClass;

                /**
                 * An accessible instance of {@link ClassLoader#getPackage(String)}.
                 */
                private final Method getPackage;

                /**
                 * An accessible instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                 */
                private final Method definePackage;

                /**
                 * Creates a new resolved reflection store.
                 *
                 * @param findLoadedClass An accessible instance of {@link ClassLoader#findLoadedClass(String)}.
                 * @param loadClass       An accessible instance of {@link ClassLoader#loadClass(String)}.
                 * @param getPackage      An accessible instance of {@link ClassLoader#getPackage(String)}.
                 * @param definePackage   An accessible instance of
                 *                        {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                 */
                protected Resolved(Method findLoadedClass, Method loadClass, Method getPackage, Method definePackage) {
                    this.findLoadedClass = findLoadedClass;
                    this.loadClass = loadClass;
                    this.getPackage = getPackage;
                    this.definePackage = definePackage;
                }

                @Override
                public Class<?> findClass(ClassLoader classLoader, String name) {
                    try {
                        return (Class<?>) findLoadedClass.invoke(classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access java.lang.ClassLoader#findClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.ClassLoader#findClass", exception.getCause());
                    }
                }

                @Override
                public Class<?> loadClass(ClassLoader classLoader,
                                          String name,
                                          byte[] binaryRepresentation,
                                          int startIndex,
                                          int endIndex,
                                          ProtectionDomain protectionDomain) {
                    try {
                        return (Class<?>) loadClass.invoke(classLoader, name, binaryRepresentation, startIndex, endIndex, protectionDomain);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access java.lang.ClassLoader#findClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.ClassLoader#findClass", exception.getCause());
                    }
                }

                @Override
                public Package getPackage(ClassLoader classLoader, String name) {
                    try {
                        return (Package) getPackage.invoke(classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access java.lang.ClassLoader#findClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.ClassLoader#findClass", exception.getCause());
                    }
                }

                @Override
                public Package definePackage(ClassLoader classLoader,
                                             String packageName,
                                             String specificationTitle,
                                             String specificationVersion,
                                             String specificationVendor,
                                             String implementationTitle,
                                             String implementationVersion,
                                             String implementationVendor,
                                             URL sealBase) {
                    try {
                        return (Package) definePackage.invoke(classLoader,
                                packageName,
                                specificationTitle,
                                specificationVersion,
                                specificationVendor,
                                implementationTitle,
                                implementationVersion,
                                implementationVendor,
                                sealBase);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access java.lang.ClassLoader#findClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.ClassLoader#findClass", exception.getCause());
                    }
                }

                @Override
                @SuppressFBWarnings(value = "DP_DO_INSIDE_DO_PRIVILEGED", justification = "Privilege is explicit user responsibility")
                public Dispatcher initialize() {
                    // This is safe even in a multi-threaded environment as all threads set the instances accessible before invoking any methods.
                    // By always setting accessability, the security manager is always triggered if this operation was illegal.
                    findLoadedClass.setAccessible(true);
                    loadClass.setAccessible(true);
                    getPackage.setAccessible(true);
                    definePackage.setAccessible(true);
                    return this;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Resolved resolved = (Resolved) other;
                    return findLoadedClass.equals(resolved.findLoadedClass)
                            && loadClass.equals(resolved.loadClass)
                            && getPackage.equals(resolved.getPackage)
                            && definePackage.equals(resolved.definePackage);
                }

                @Override
                public int hashCode() {
                    int result = findLoadedClass.hashCode();
                    result = 31 * result + loadClass.hashCode();
                    result = 31 * result + getPackage.hashCode();
                    result = 31 * result + definePackage.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "ClassInjector.UsingReflection.Dispatcher.Resolved{" +
                            "findLoadedClass=" + findLoadedClass +
                            ", loadClass=" + loadClass +
                            ", getPackage=" + getPackage +
                            ", definePackage=" + definePackage +
                            '}';
                }
            }

            /**
             * Represents an unsuccessfully loaded method lookup.
             */
            class Faulty implements Initializable {

                /**
                 * The exception that occurred when looking up the reflection methods.
                 */
                private final Exception exception;

                /**
                 * Creates a new faulty reflection store.
                 *
                 * @param exception The exception that was thrown when attempting to lookup the method.
                 */
                protected Faulty(Exception exception) {
                    this.exception = exception;
                }

                @Override
                public Dispatcher initialize() {
                    throw new IllegalStateException("Error locating class loader API", exception);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && exception.equals(((Faulty) other).exception);
                }

                @Override
                public int hashCode() {
                    return exception.hashCode();
                }

                @Override
                public String toString() {
                    return "ClassInjector.UsingReflection.Dispatcher.Faulty{exception=" + exception + '}';
                }
            }
        }
    }

    /**
     * A class injector using a {@link java.lang.instrument.Instrumentation} to append to either the boot classpath
     * or the system class path.
     */
    class UsingInstrumentation implements ClassInjector {

        /**
         * A prefix to use of generated files.
         */
        private static final String PREFIX = "jar";

        /**
         * The class file extension.
         */
        private static final String CLASS_FILE_EXTENSION = ".class";

        /**
         * The instrumentation to use for appending to the class path or the boot path.
         */
        private final Instrumentation instrumentation;

        /**
         * A representation of the target path to which classes are to be appended.
         */
        private final Target target;

        /**
         * The folder to be used for storing jar files.
         */
        private final File folder;

        /**
         * A random string generator for creating file names.
         */
        private final RandomString randomString;

        /**
         * Creates an instrumentation-based class injector.
         *
         * @param folder          The folder to be used for storing jar files.
         * @param target          A representation of the target path to which classes are to be appended.
         * @param instrumentation The instrumentation to use for appending to the class path or the boot path.
         * @return An appropriate class injector that applies instrumentation.
         */
        public static ClassInjector of(File folder, Target target, Instrumentation instrumentation) {
            return new UsingInstrumentation(folder, target, instrumentation, new RandomString());
        }

        /**
         * Creates an instrumentation-based class injector.
         *
         * @param folder          The folder to be used for storing jar files.
         * @param target          A representation of the target path to which classes are to be appended.
         * @param instrumentation The instrumentation to use for appending to the class path or the boot path.
         * @param randomString    The random string generator to use.
         */
        protected UsingInstrumentation(File folder,
                                       Target target,
                                       Instrumentation instrumentation,
                                       RandomString randomString) {
            this.folder = folder;
            this.target = target;
            this.instrumentation = instrumentation;
            this.randomString = randomString;
        }

        @Override
        public Map<TypeDescription, Class<?>> inject(Map<? extends TypeDescription, byte[]> types) {
            File jarFile = new File(folder, String.format("%s%s.jar", PREFIX, randomString.nextString()));
            try {
                if (!jarFile.createNewFile()) {
                    throw new IllegalStateException("Cannot create file " + jarFile);
                }
                JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile));
                try {
                    for (Map.Entry<? extends TypeDescription, byte[]> entry : types.entrySet()) {
                        jarOutputStream.putNextEntry(new JarEntry(entry.getKey().getInternalName() + CLASS_FILE_EXTENSION));
                        jarOutputStream.write(entry.getValue());
                    }
                } finally {
                    jarOutputStream.close();
                }
                target.inject(instrumentation, new JarFile(jarFile));
                Map<TypeDescription, Class<?>> loaded = new HashMap<TypeDescription, Class<?>>();
                for (TypeDescription typeDescription : types.keySet()) {
                    loaded.put(typeDescription, Class.forName(typeDescription.getName(), false, ClassLoader.getSystemClassLoader()));
                }
                return loaded;
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot write jar file to disk", exception);
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Cannot load injected class", exception);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            UsingInstrumentation that = (UsingInstrumentation) other;
            return folder.equals(that.folder)
                    && instrumentation.equals(that.instrumentation)
                    && target == that.target
                    && randomString.equals(that.randomString);
        }

        @Override
        public int hashCode() {
            int result = instrumentation.hashCode();
            result = 31 * result + target.hashCode();
            result = 31 * result + folder.hashCode();
            result = 31 * result + randomString.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ClassInjector.UsingInstrumentation{" +
                    "instrumentation=" + instrumentation +
                    ", target=" + target +
                    ", folder=" + folder +
                    ", randomString=" + randomString +
                    '}';
        }

        /**
         * A representation of the target to which Java classes should be appended to.
         */
        public enum Target {

            /**
             * Representation of the bootstrap class loader.
             */
            BOOTSTRAP {
                @Override
                protected void inject(Instrumentation instrumentation, JarFile jarFile) {
                    instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
                }
            },

            /**
             * Representation of the system class loader.
             */
            SYSTEM {
                @Override
                protected void inject(Instrumentation instrumentation, JarFile jarFile) {
                    instrumentation.appendToSystemClassLoaderSearch(jarFile);
                }
            };

            /**
             * Adds the given classes to the represented class loader.
             *
             * @param instrumentation The instrumentation instance to use.
             * @param jarFile         The jar file to append.
             */
            protected abstract void inject(Instrumentation instrumentation, JarFile jarFile);

            @Override
            public String toString() {
                return "ClassInjector.UsingInstrumentation.Target." + name();
            }
        }
    }
}
