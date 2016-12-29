package net.bytebuddy.dynamic.loading;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.RandomString;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
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
         * The dispatcher to use for accessing a class loader via reflection.
         */
        private static final Dispatcher.Initializable DISPATCHER = Dispatcher.Resolved.make();

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
         * @param classLoader The {@link java.lang.ClassLoader} into which new class definitions are to be injected. Must not be the bootstrap loader.
         */
        public UsingReflection(ClassLoader classLoader) {
            this(classLoader, DEFAULT_PROTECTION_DOMAIN);
        }

        /**
         * Creates a new injector for the given {@link java.lang.ClassLoader} and a default {@link PackageDefinitionStrategy} where the
         * injection of existent classes does not trigger an error.
         *
         * @param classLoader      The {@link java.lang.ClassLoader} into which new class definitions are to be injected. Must not be the bootstrap loader.
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
         * @param classLoader               The {@link java.lang.ClassLoader} into which new class definitions are to be injected.Must  not be the bootstrap loader.
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
            Dispatcher dispatcher = DISPATCHER.initialize();
            Map<TypeDescription, Class<?>> loadedTypes = new HashMap<TypeDescription, Class<?>>();
            for (Map.Entry<? extends TypeDescription, byte[]> entry : types.entrySet()) {
                String typeName = entry.getKey().getName();
                synchronized (dispatcher.getClassLoadingLock(classLoader, typeName)) {
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
                        type = dispatcher.defineClass(classLoader, typeName, entry.getValue(), protectionDomain);
                    } else if (forbidExisting) {
                        throw new IllegalStateException("Cannot inject already loaded type: " + type);
                    }
                    loadedTypes.put(entry.getKey(), type);
                }
            }
            return loadedTypes;
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
             * Indicates a class that is currently not defined.
             */
            Class<?> UNDEFINED = null;

            Object getClassLoadingLock(ClassLoader classLoader, String name);

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
             * @param protectionDomain     The protection domain for the defined class.
             * @return The defined, loaded class.
             */
            Class<?> defineClass(ClassLoader classLoader, String name, byte[] binaryRepresentation, ProtectionDomain protectionDomain);

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
             * @param name                  The binary name of the package.
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
                                  String name,
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
            abstract class Resolved implements Dispatcher, Initializable {

                /**
                 * Indicates an reflective access on a static member.
                 */
                private static final Object STATIC_MEMBER = null;

                /**
                 * An instance of {@link ClassLoader#findLoadedClass(String)}.
                 */
                protected final Method findLoadedClass;

                /**
                 * An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                 */
                protected final Method defineClass;

                /**
                 * An instance of {@link ClassLoader#getPackage(String)} or {@code ClassLoader#getDefinedPackage(String)}.
                 */
                protected final Method getPackage;

                /**
                 * An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                 */
                protected final Method definePackage;

                /**
                 * An instance of {@code sun.misc.Unsafe#theUnsafe} or {@code null} if it is not available.
                 */
                protected final Field theUnsafe;

                /**
                 * An instance of {@code sun.misc.Unsafe#defineClass(String, byte[], int, int, ClassLoader, ProtectionDomain)}
                 * or {@code null} if it is not available.
                 */
                protected final Method defineClassUnsafe;

                /**
                 * Creates a new resolved reflection store.
                 *
                 * @param findLoadedClass   An instance of {@link ClassLoader#findLoadedClass(String)}.
                 * @param defineClass       An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                 * @param getPackage        An instance of {@link ClassLoader#getPackage(String)} or {@code ClassLoader#getDefinedPackage(String)}.
                 * @param definePackage     An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                 * @param defineClassUnsafe An instance of {@code sun.misc.Unsafe#theUnsafe} or {@code null} if it is not available.
                 * @param theUnsafe         An instance of {@code sun.misc.Unsafe#defineClass(String, byte[], int, int, ClassLoader, ProtectionDomain)}
                 *                          or {@code null} if it is not available.
                 */
                protected Resolved(Method findLoadedClass,
                                   Method defineClass,
                                   Method getPackage,
                                   Method definePackage,
                                   Field theUnsafe,
                                   Method defineClassUnsafe) {
                    this.findLoadedClass = findLoadedClass;
                    this.defineClass = defineClass;
                    this.getPackage = getPackage;
                    this.definePackage = definePackage;
                    this.theUnsafe = theUnsafe;
                    this.defineClassUnsafe = defineClassUnsafe;
                }

                /**
                 * Obtains the reflective instances used by this injector or a no-op instance that throws the exception
                 * that occurred when attempting to obtain the reflective member instances.
                 *
                 * @return A dispatcher for the current VM.
                 */
                @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
                protected static Dispatcher.Initializable make() {
                    Field theUnsafe;
                    Method defineClass;
                    try {
                        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
                        theUnsafe = unsafe.getDeclaredField("theUnsafe");
                        defineClass = unsafe.getDeclaredMethod("defineClass",
                                String.class,
                                byte[].class,
                                int.class,
                                int.class,
                                ClassLoader.class,
                                ProtectionDomain.class);
                    } catch (Throwable ignored) {
                        theUnsafe = null;
                        defineClass = null;
                    }
                    try {
                        Method getPackage;
                        try {
                            getPackage = ClassLoader.class.getDeclaredMethod("getDefinedPackage", String.class);
                        } catch (NoSuchMethodException ignored) {
                            getPackage = ClassLoader.class.getDeclaredMethod("getPackage", String.class);
                        }
                        try {
                            return new Dispatcher.Resolved.ForJava7CapableVm(ClassLoader.class.getDeclaredMethod("getClassLoadingLock", String.class),
                                    ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class),
                                    ClassLoader.class.getDeclaredMethod("defineClass",
                                            String.class,
                                            byte[].class,
                                            int.class,
                                            int.class,
                                            ProtectionDomain.class),
                                    getPackage,
                                    ClassLoader.class.getDeclaredMethod("definePackage",
                                            String.class,
                                            String.class,
                                            String.class,
                                            String.class,
                                            String.class,
                                            String.class,
                                            String.class,
                                            URL.class),
                                    theUnsafe,
                                    defineClass);
                        } catch (NoSuchMethodException ignored) {
                            return new Dispatcher.Resolved.ForLegacyVm(ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class),
                                    ClassLoader.class.getDeclaredMethod("defineClass",
                                            String.class,
                                            byte[].class,
                                            int.class,
                                            int.class,
                                            ProtectionDomain.class),
                                    getPackage,
                                    ClassLoader.class.getDeclaredMethod("definePackage",
                                            String.class,
                                            String.class,
                                            String.class,
                                            String.class,
                                            String.class,
                                            String.class,
                                            String.class,
                                            URL.class),
                                    theUnsafe,
                                    defineClass);
                        }
                    } catch (Exception exception) {
                        return new Dispatcher.Faulty(exception);
                    }
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
                public Class<?> defineClass(ClassLoader classLoader, String name, byte[] binaryRepresentation, ProtectionDomain protectionDomain) {
                    try {
                        return (Class<?>) defineClass.invoke(classLoader, name, binaryRepresentation, 0, binaryRepresentation.length, protectionDomain);
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
                                             String name,
                                             String specificationTitle,
                                             String specificationVersion,
                                             String specificationVendor,
                                             String implementationTitle,
                                             String implementationVersion,
                                             String implementationVendor,
                                             URL sealBase) {
                    try {
                        return (Package) definePackage.invoke(classLoader,
                                name,
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
                @SuppressFBWarnings(value = {"DP_DO_INSIDE_DO_PRIVILEGED", "REC_CATCH_EXCEPTION"}, justification = "Privilege is explicit user responsibility")
                public Dispatcher initialize() {
                    try {
                        // This is safe even in a multi-threaded environment as all threads set the instances accessible before invoking any methods.
                        // By always setting accessibility, the security manager is always triggered if this operation was illegal.
                        findLoadedClass.setAccessible(true);
                        defineClass.setAccessible(true);
                        getPackage.setAccessible(true);
                        definePackage.setAccessible(true);
                        onInitialization();
                        return this;
                    } catch (RuntimeException exception) {
                        if (theUnsafe == null) {
                            return new Faulty(exception);
                        } else {
                            try {
                                // This serves as a fallback on environments where accessible reflection on the class loader methods is not available.
                                theUnsafe.setAccessible(true);
                                return new UnsafeDispatcher(theUnsafe.get(STATIC_MEMBER), defineClassUnsafe);
                            } catch (Exception ignored) {
                                return new Faulty(exception);
                            }
                        }
                    }
                }

                /**
                 * Invoked upon initializing methods.
                 */
                protected abstract void onInitialization();

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Resolved resolved = (Resolved) other;
                    return findLoadedClass.equals(resolved.findLoadedClass)
                            && defineClass.equals(resolved.defineClass)
                            && getPackage.equals(resolved.getPackage)
                            && definePackage.equals(resolved.definePackage)
                            && theUnsafe.equals(resolved.theUnsafe)
                            && defineClassUnsafe.equals(resolved.defineClassUnsafe);
                }

                @Override
                public int hashCode() {
                    int result = findLoadedClass.hashCode();
                    result = 31 * result + defineClass.hashCode();
                    result = 31 * result + getPackage.hashCode();
                    result = 31 * result + definePackage.hashCode();
                    result = 31 * result + theUnsafe.hashCode();
                    result = 31 * result + defineClassUnsafe.hashCode();
                    return result;
                }

                /**
                 * A dispatcher that uses {@code sun.misc.Unsafe} for class loading but which is incapable of defining packages.
                 */
                protected static class UnsafeDispatcher implements Dispatcher {

                    /**
                     * An instance of {@code sun.misc.Unsafe}.
                     */
                    private final Object unsafe;

                    /**
                     * An instance of {@code sun.misc.Unsafe#defineClass(String, byte[], int, int, ClassLoader, ProtectionDomain)}.
                     */
                    private final Method defineClass;

                    /**
                     * Creates a new unsafe dispatcher.
                     *
                     * @param unsafe      An instance of {@code sun.misc.Unsafe}.
                     * @param defineClass An instance of {@code sun.misc.Unsafe#defineClass(String, byte[], int, int, ClassLoader, ProtectionDomain)}.
                     */
                    protected UnsafeDispatcher(Object unsafe, Method defineClass) {
                        this.unsafe = unsafe;
                        this.defineClass = defineClass;
                    }

                    @Override
                    public Object getClassLoadingLock(ClassLoader classLoader, String name) {
                        return classLoader;
                    }

                    @Override
                    public Class<?> findClass(ClassLoader classLoader, String name) {
                        try {
                            return classLoader.loadClass(name);
                        } catch (ClassNotFoundException ignored) {
                            return UNDEFINED;
                        }
                    }

                    @Override
                    public Class<?> defineClass(ClassLoader classLoader, String name, byte[] binaryRepresentation, ProtectionDomain protectionDomain) {
                        try {
                            return (Class<?>) defineClass.invoke(unsafe,
                                    name,
                                    binaryRepresentation,
                                    0,
                                    binaryRepresentation.length,
                                    classLoader,
                                    protectionDomain);
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException("Could not access com.sun.Unsafe#defineClass", exception);
                        } catch (InvocationTargetException exception) {
                            throw new IllegalStateException("Error invoking com.sun.Unsafe#defineClass", exception.getCause());
                        }
                    }

                    @Override
                    public Package getPackage(ClassLoader classLoader, String name) {
                        throw new UnsupportedOperationException("Cannot get package using unsafe injection dispatcher: " + name);
                    }

                    @Override
                    public Package definePackage(ClassLoader classLoader,
                                                 String name,
                                                 String specificationTitle,
                                                 String specificationVersion,
                                                 String specificationVendor,
                                                 String implementationTitle,
                                                 String implementationVersion,
                                                 String implementationVendor,
                                                 URL sealBase) {
                        throw new UnsupportedOperationException("Cannot define package using unsafe injection dispatcher: " + name);
                    }

                    @Override
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        UnsafeDispatcher that = (UnsafeDispatcher) object;
                        return unsafe.equals(that.unsafe) && defineClass.equals(that.defineClass);
                    }

                    @Override
                    public int hashCode() {
                        int result = unsafe.hashCode();
                        result = 31 * result + defineClass.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "ClassInjector.UsingReflection.Dispatcher.Resolved.UnsafeDispatcher{" +
                                "unsafe=" + unsafe +
                                ", defineClass=" + defineClass +
                                '}';
                    }
                }

                /**
                 * A resolved class dispatcher for a class injector on a VM running at least Java 7.
                 */
                protected static class ForJava7CapableVm extends Resolved {

                    /**
                     * An instance of {@code ClassLoader#getClassLoadingLock(String)}.
                     */
                    private final Method getClassLoadingLock;

                    /**
                     * Creates a new resolved reflection store for a VM running at least Java 7.
                     *
                     * @param getClassLoadingLock An instance of {@code ClassLoader#getClassLoadingLock(String)}.
                     * @param findLoadedClass     An instance of {@link ClassLoader#findLoadedClass(String)}.
                     * @param defineClass         An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                     * @param getPackage          An instance of {@link ClassLoader#getPackage(String)} or {@code ClassLoader#getDefinedPackage(String)}.
                     * @param definePackage       An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                     * @param defineClassUnsafe   An instance of {@code sun.misc.Unsafe#theUnsafe} or {@code null} if it is not available.
                     * @param theUnsafe           An instance of {@code sun.misc.Unsafe#defineClass(String, byte[], int, int, ClassLoader, ProtectionDomain)}
                     *                            or {@code null} if it is not available.
                     */
                    protected ForJava7CapableVm(Method getClassLoadingLock,
                                                Method findLoadedClass,
                                                Method defineClass,
                                                Method getPackage,
                                                Method definePackage,
                                                Field theUnsafe,
                                                Method defineClassUnsafe) {
                        super(findLoadedClass, defineClass, getPackage, definePackage, theUnsafe, defineClassUnsafe);
                        this.getClassLoadingLock = getClassLoadingLock;
                    }

                    @Override
                    public Object getClassLoadingLock(ClassLoader classLoader, String name) {
                        try {
                            return getClassLoadingLock.invoke(classLoader, name);
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException("Could not access java.lang.ClassLoader#getClassLoadingLock", exception);
                        } catch (InvocationTargetException exception) {
                            throw new IllegalStateException("Error invoking java.lang.ClassLoader#getClassLoadingLock", exception.getCause());
                        }
                    }

                    @Override
                    @SuppressFBWarnings(value = "DP_DO_INSIDE_DO_PRIVILEGED", justification = "Privilege is explicit user responsibility")
                    protected void onInitialization() {
                        getClassLoadingLock.setAccessible(true);
                    }

                    @Override
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        if (!super.equals(object)) return false;
                        ForJava7CapableVm that = (ForJava7CapableVm) object;
                        return getClassLoadingLock.equals(that.getClassLoadingLock);
                    }

                    @Override
                    public int hashCode() {
                        int result = super.hashCode();
                        result = 31 * result + getClassLoadingLock.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "ClassInjector.UsingReflection.Dispatcher.Resolved.ForJava7CapableVm{" +
                                "findLoadedClass=" + findLoadedClass +
                                ", defineClass=" + defineClass +
                                ", getPackage=" + getPackage +
                                ", definePackage=" + definePackage +
                                ", theUnsafe=" + theUnsafe +
                                ", defineClassUnsafe=" + defineClassUnsafe +
                                ", getClassLoadingLock=" + getClassLoadingLock +
                                '}';
                    }
                }

                /**
                 * A resolved class dispatcher for a class injector prior to Java 7.
                 */
                protected static class ForLegacyVm extends Resolved {

                    /**
                     * Creates a new resolved reflection store for a VM prior to Java 8.
                     *
                     * @param findLoadedClass   An instance of {@link ClassLoader#findLoadedClass(String)}.
                     * @param defineClass       An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                     * @param getPackage        An instance of {@link ClassLoader#getPackage(String)} or {@code ClassLoader#getDefinedPackage(String)}.
                     * @param definePackage     An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                     * @param defineClassUnsafe An instance of {@code sun.misc.Unsafe#theUnsafe} or {@code null} if it is not available.
                     * @param theUnsafe         An instance of {@code sun.misc.Unsafe#defineClass(String, byte[], int, int, ClassLoader, ProtectionDomain)}
                     *                          or {@code null} if it is not available.
                     */
                    protected ForLegacyVm(Method findLoadedClass,
                                          Method defineClass,
                                          Method getPackage,
                                          Method definePackage,
                                          Field theUnsafe,
                                          Method defineClassUnsafe) {
                        super(findLoadedClass, defineClass, getPackage, definePackage, theUnsafe, defineClassUnsafe);
                    }

                    @Override
                    public Object getClassLoadingLock(ClassLoader classLoader, String name) {
                        return classLoader;
                    }

                    @Override
                    protected void onInitialization() {
                        /* do nothing */
                    }

                    @Override
                    public String toString() {
                        return "ClassInjector.UsingReflection.Dispatcher.Resolved.ForLegacyVm{" +
                                "findLoadedClass=" + findLoadedClass +
                                ", defineClass=" + defineClass +
                                ", getPackage=" + getPackage +
                                ", definePackage=" + definePackage +
                                ", theUnsafe=" + theUnsafe +
                                ", defineClassUnsafe=" + defineClassUnsafe +
                                '}';
                    }
                }
            }

            /**
             * Represents an unsuccessfully loaded method lookup.
             */
            class Faulty implements Dispatcher, Initializable {

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
                    return this;
                }

                @Override
                public Object getClassLoadingLock(ClassLoader classLoader, String name) {
                    return classLoader;
                }

                @Override
                public Class<?> findClass(ClassLoader classLoader, String name) {
                    try {
                        return classLoader.loadClass(name);
                    } catch (ClassNotFoundException ignored) {
                        return UNDEFINED;
                    }
                }

                @Override
                public Class<?> defineClass(ClassLoader classLoader, String name, byte[] binaryRepresentation, ProtectionDomain protectionDomain) {
                    throw new UnsupportedOperationException("Cannot define class using reflection", exception);
                }

                @Override
                public Package getPackage(ClassLoader classLoader, String name) {
                    throw new UnsupportedOperationException("Cannot get package using reflection", exception);
                }

                @Override
                public Package definePackage(ClassLoader classLoader,
                                             String name,
                                             String specificationTitle,
                                             String specificationVersion,
                                             String specificationVendor,
                                             String implementationTitle,
                                             String implementationVersion,
                                             String implementationVendor,
                                             URL sealBase) {
                    throw new UnsupportedOperationException("Cannot define package using injection", exception);
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
