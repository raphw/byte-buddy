package net.bytebuddy.dynamic.loading;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.EqualsAndHashCode;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.JavaType;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ReflectPermission;
import java.net.URL;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
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
     * Determines the default behavior for type injections when a type is already loaded.
     */
    boolean ALLOW_EXISTING_TYPES = false;

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
    @EqualsAndHashCode
    class UsingReflection implements ClassInjector {

        /**
         * The dispatcher to use for accessing a class loader via reflection.
         */
        private static final Dispatcher.Initializable DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

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
            this(classLoader, ClassLoadingStrategy.NO_PROTECTION_DOMAIN);
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
                    ALLOW_EXISTING_TYPES);
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
         * Indicates if this class injection is available on the current VM.
         *
         * @return {@code true} if this class injection is available.
         */
        public static boolean isAvailable() {
            return DISPATCHER.isAvailable();
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

        /**
         * A dispatcher for accessing a {@link ClassLoader} reflectively.
         */
        protected interface Dispatcher {

            /**
             * Indicates a class that is currently not defined.
             */
            Class<?> UNDEFINED = null;

            /**
             * Returns the lock for loading the specified class.
             *
             * @param classLoader the class loader to inject the class into.
             * @param name        The name of the class.
             * @return The lock for loading this class.
             */
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
                 * Indicates if this dispatcher is available.
                 *
                 * @return {@code true} if this dispatcher is available.
                 */
                boolean isAvailable();

                /**
                 * Initializes this dispatcher.
                 *
                 * @return The initialized dispatcher.
                 */
                Dispatcher initialize();
            }

            /**
             * A creation action for a dispatcher.
             */
            enum CreationAction implements PrivilegedAction<Initializable> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
                public Initializable run() {
                    try {
                        return JavaModule.isSupported()
                                ? Dispatcher.Indirect.make()
                                : Dispatcher.Direct.make();
                    } catch (Exception exception) {
                        return new Unavailable(exception);
                    }
                }
            }

            /**
             * A class injection dispatcher that is using reflection on the {@link ClassLoader} methods.
             */
            @EqualsAndHashCode
            abstract class Direct implements Dispatcher, Initializable {

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
                 * Creates a new direct injection dispatcher.
                 *
                 * @param findLoadedClass An instance of {@link ClassLoader#findLoadedClass(String)}.
                 * @param defineClass     An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                 * @param getPackage      An instance of {@link ClassLoader#getPackage(String)} or {@code ClassLoader#getDefinedPackage(String)}.
                 * @param definePackage   An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                 */
                protected Direct(Method findLoadedClass,
                                 Method defineClass,
                                 Method getPackage,
                                 Method definePackage) {
                    this.findLoadedClass = findLoadedClass;
                    this.defineClass = defineClass;
                    this.getPackage = getPackage;
                    this.definePackage = definePackage;
                }

                /**
                 * Creates a direct dispatcher.
                 *
                 * @return A direct dispatcher for class injection.
                 * @throws Exception If the creation is impossible.
                 */
                protected static Initializable make() throws Exception {
                    Method getPackage;
                    try {
                        getPackage = ClassLoader.class.getDeclaredMethod("getDefinedPackage", String.class);
                    } catch (NoSuchMethodException ignored) {
                        getPackage = ClassLoader.class.getDeclaredMethod("getPackage", String.class);
                    }
                    Method findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
                    Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass",
                            String.class,
                            byte[].class,
                            int.class,
                            int.class,
                            ProtectionDomain.class);
                    Method definePackage = ClassLoader.class.getDeclaredMethod("definePackage",
                            String.class,
                            String.class,
                            String.class,
                            String.class,
                            String.class,
                            String.class,
                            String.class,
                            URL.class);
                    try {
                        return new ForJava7CapableVm(findLoadedClass,
                                defineClass,
                                getPackage,
                                definePackage,
                                ClassLoader.class.getDeclaredMethod("getClassLoadingLock", String.class));
                    } catch (NoSuchMethodException ignored) {
                        return new ForLegacyVm(findLoadedClass, defineClass, getPackage, definePackage);
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
                        throw new IllegalStateException("Could not access java.lang.ClassLoader#defineClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.ClassLoader#defineClass", exception.getCause());
                    }
                }

                @Override
                public Package getPackage(ClassLoader classLoader, String name) {
                    try {
                        return (Package) getPackage.invoke(classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access java.lang.ClassLoader#getPackage", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.ClassLoader#getPackage", exception.getCause());
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
                        throw new IllegalStateException("Could not access java.lang.ClassLoader#definePackage", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.ClassLoader#definePackage", exception.getCause());
                    }
                }

                @Override
                public boolean isAvailable() {
                    return true;
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
                    } catch (Exception exception) {
                        return new Unavailable(exception);
                    }
                }

                /**
                 * Invoked upon initializing methods.
                 */
                protected abstract void onInitialization();

                /**
                 * A resolved class dispatcher for a class injector on a VM running at least Java 7.
                 */
                @EqualsAndHashCode(callSuper = true)
                protected static class ForJava7CapableVm extends Direct {

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
                     */
                    protected ForJava7CapableVm(Method findLoadedClass,
                                                Method defineClass,
                                                Method getPackage,
                                                Method definePackage,
                                                Method getClassLoadingLock) {
                        super(findLoadedClass, defineClass, getPackage, definePackage);
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
                }

                /**
                 * A resolved class dispatcher for a class injector prior to Java 7.
                 */
                protected static class ForLegacyVm extends Direct {

                    /**
                     * Creates a new resolved reflection store for a VM prior to Java 8.
                     *
                     * @param findLoadedClass An instance of {@link ClassLoader#findLoadedClass(String)}.
                     * @param defineClass     An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                     * @param getPackage      An instance of {@link ClassLoader#getPackage(String)} or {@code ClassLoader#getDefinedPackage(String)}.
                     * @param definePackage   An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                     */
                    protected ForLegacyVm(Method findLoadedClass,
                                          Method defineClass,
                                          Method getPackage,
                                          Method definePackage) {
                        super(findLoadedClass, defineClass, getPackage, definePackage);
                    }

                    @Override
                    public Object getClassLoadingLock(ClassLoader classLoader, String name) {
                        return classLoader;
                    }

                    @Override
                    protected void onInitialization() {
                        /* do nothing */
                    }
                }
            }

            /**
             * An indirect dispatcher that uses a redirection accessor class that was injected into the bootstrap class loader.
             */
            @EqualsAndHashCode
            class Indirect implements Dispatcher, Initializable {

                /**
                 * The access permission to check upon injection if a security manager is active.
                 */
                private static final Permission ACCESS_PERMISSION = new ReflectPermission("suppressAccessChecks");

                /**
                 * An instance of the accessor class that is required for using it's intentionally non-static methods.
                 */
                private final Object accessor;

                /**
                 * The accessor method for using {@link ClassLoader#findLoadedClass(String)}.
                 */
                private final Method findLoadedClass;

                /**
                 * The accessor method for using {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                 */
                private final Method defineClass;

                /**
                 * The accessor method for using {@link ClassLoader#getPackage(String)} or {@code ClassLoader#getDefinedPackage(String)}.
                 */
                private final Method getPackage;

                /**
                 * The accessor method for using {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                 */
                private final Method definePackage;

                /**
                 * The accessor method for using {@code ClassLoader#getClassLoadingLock(String)} or returning the supplied {@link ClassLoader}
                 * if this method does not exist on the current VM.
                 */
                private final Method getClassLoadingLock;

                /**
                 * Creates a new indirect class loading injection dispatcher.
                 *
                 * @param accessor            An instance of the accessor class that is required for using it's intentionally non-static methods.
                 * @param findLoadedClass     An instance of {@link ClassLoader#findLoadedClass(String)}.
                 * @param defineClass         An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                 * @param getPackage          An instance of {@link ClassLoader#getPackage(String)} or {@code ClassLoader#getDefinedPackage(String)}.
                 * @param definePackage       An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                 * @param getClassLoadingLock The accessor method for using {@code ClassLoader#getClassLoadingLock(String)} or returning the
                 *                            supplied {@link ClassLoader} if this method does not exist on the current VM.
                 */
                protected Indirect(Object accessor,
                                   Method findLoadedClass,
                                   Method defineClass,
                                   Method getPackage,
                                   Method definePackage,
                                   Method getClassLoadingLock) {
                    this.accessor = accessor;
                    this.findLoadedClass = findLoadedClass;
                    this.defineClass = defineClass;
                    this.getPackage = getPackage;
                    this.definePackage = definePackage;
                    this.getClassLoadingLock = getClassLoadingLock;
                }

                @Override
                public boolean isAvailable() {
                    return true;
                }

                /**
                 * Creates an indirect dispatcher.
                 *
                 * @return An indirect dispatcher for class creation.
                 * @throws Exception If the dispatcher cannot be created.
                 */
                @SuppressFBWarnings(value = "DP_DO_INSIDE_DO_PRIVILEGED", justification = "Privilege is explicit caller responsibility")
                public static Initializable make() throws Exception {
                    Class<?> unsafe = Class.forName("sun.misc.Unsafe");
                    Field theUnsafe = unsafe.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    Object unsafeInstance = theUnsafe.get(null);
                    Method getPackage;
                    try {
                        getPackage = ClassLoader.class.getDeclaredMethod("getDeclaredPackage", String.class);
                    } catch (NoSuchMethodException ignored) {
                        getPackage = ClassLoader.class.getDeclaredMethod("getPackage", String.class);
                    }
                    DynamicType.Builder<?> builder = new ByteBuddy()
                            .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                            .name(ClassLoader.class.getName() + "$ByteBuddyAccessor$" + RandomString.make())
                            .defineMethod("findLoadedClass", Class.class, Visibility.PUBLIC)
                            .withParameters(ClassLoader.class, String.class)
                            .intercept(MethodCall.invoke(ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class))
                                    .onArgument(0)
                                    .withArgument(1))
                            .defineMethod("defineClass", Class.class, Visibility.PUBLIC)
                            .withParameters(ClassLoader.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class)
                            .intercept(MethodCall.invoke(ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class))
                                    .onArgument(0)
                                    .withArgument(1, 2, 3, 4, 5))
                            .defineMethod("getPackage", Package.class, Visibility.PUBLIC)
                            .withParameters(ClassLoader.class, String.class)
                            .intercept(MethodCall.invoke(getPackage)
                                    .onArgument(0)
                                    .withArgument(1))
                            .defineMethod("definePackage", Package.class, Visibility.PUBLIC)
                            .withParameters(ClassLoader.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, URL.class)
                            .intercept(MethodCall.invoke(ClassLoader.class.getDeclaredMethod("definePackage", String.class, String.class, String.class, String.class, String.class, String.class, String.class, URL.class))
                                    .onArgument(0)
                                    .withArgument(1, 2, 3, 4, 5, 6, 7, 8));
                    try {
                        builder = builder.defineMethod("getClassLoadingLock", Object.class, Visibility.PUBLIC)
                                .withParameters(ClassLoader.class, String.class)
                                .intercept(MethodCall.invoke(ClassLoader.class.getDeclaredMethod("getClassLoadingLock", String.class))
                                        .onArgument(0)
                                        .withArgument(1));
                    } catch (NoSuchMethodException ignored) {
                        builder = builder.defineMethod("getClassLoadingLock", Object.class, Visibility.PUBLIC)
                                .withParameters(ClassLoader.class, String.class)
                                .intercept(FixedValue.argument(0));
                    }
                    Class<?> type = builder.make().load(ClassLoadingStrategy.BOOTSTRAP_LOADER, new ClassLoadingStrategy.ForUnsafeInjection()).getLoaded();
                    return new Indirect(unsafe.getDeclaredMethod("allocateInstance", Class.class).invoke(unsafeInstance, type),
                            type.getMethod("findLoadedClass", ClassLoader.class, String.class),
                            type.getMethod("defineClass", ClassLoader.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class),
                            type.getMethod("getPackage", ClassLoader.class, String.class),
                            type.getMethod("definePackage", ClassLoader.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, URL.class),
                            type.getMethod("getClassLoadingLock", ClassLoader.class, String.class));
                }

                @Override
                public Dispatcher initialize() {
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null) {
                        securityManager.checkPermission(ACCESS_PERMISSION);
                    }
                    return this;
                }

                @Override
                public Object getClassLoadingLock(ClassLoader classLoader, String name) {
                    try {
                        return getClassLoadingLock.invoke(accessor, classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access (accessor)::getClassLoadingLock", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking (accessor)::getClassLoadingLock", exception.getCause());
                    }
                }

                @Override
                public Class<?> findClass(ClassLoader classLoader, String name) {
                    try {
                        return (Class<?>) findLoadedClass.invoke(accessor, classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access (accessor)::findLoadedClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking (accessor)::findLoadedClass", exception.getCause());
                    }
                }

                @Override
                public Class<?> defineClass(ClassLoader classLoader, String name, byte[] binaryRepresentation, ProtectionDomain protectionDomain) {
                    try {
                        return (Class<?>) defineClass.invoke(accessor, classLoader, name, binaryRepresentation, 0, binaryRepresentation.length, protectionDomain);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access (accessor)::defineClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking (accessor)::defineClass", exception.getCause());
                    }
                }

                @Override
                public Package getPackage(ClassLoader classLoader, String name) {
                    try {
                        return (Package) getPackage.invoke(accessor, classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access (accessor)::getPackage", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking (accessor)::getPackage", exception.getCause());
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
                        return (Package) definePackage.invoke(accessor,
                                classLoader,
                                name,
                                specificationTitle,
                                specificationVersion,
                                specificationVendor,
                                implementationTitle,
                                implementationVersion,
                                implementationVendor,
                                sealBase);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access (accessor)::definePackage", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking (accessor)::definePackage", exception.getCause());
                    }
                }
            }

            /**
             * Represents an unsuccessfully loaded method lookup.
             */
            @EqualsAndHashCode
            class Unavailable implements Dispatcher, Initializable {

                /**
                 * The exception that occurred when looking up the reflection methods.
                 */
                private final Exception exception;

                /**
                 * Creates a new faulty reflection store.
                 *
                 * @param exception The exception that was thrown when attempting to lookup the method.
                 */
                protected Unavailable(Exception exception) {
                    this.exception = exception;
                }

                @Override
                public boolean isAvailable() {
                    return false;
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
            }
        }
    }

    /**
     * <p>
     * A class injector that uses a {@code java.lang.invoke.MethodHandles$Lookup} object for defining a class.
     * </p>
     * <p>
     * <b>Important</b>: This functionality is only available starting from Java 9.
     * </p>
     */
    @EqualsAndHashCode
    class UsingLookup implements ClassInjector {

        /**
         * The dispatcher to interacting with method handles.
         */
        private static final Dispatcher DISPATCHER = AccessController.doPrivileged(Dispatcher.Creator.INSTANCE);

        /**
         * Indicates a lookup instance's package lookup mode.
         */
        private static final int PACKAGE_LOOKUP = 0x8;

        /**
         * The {@code java.lang.invoke.MethodHandles$Lookup} to use.
         */
        private final Object lookup;

        /**
         * Creates a new class injector using a lookup instance.
         *
         * @param lookup The {@code java.lang.invoke.MethodHandles$Lookup} instance to use.
         */
        protected UsingLookup(Object lookup) {
            this.lookup = lookup;
        }

        /**
         * Creates class injector that defines a class using a method handle lookup.
         *
         * @param lookup The {@code java.lang.invoke.MethodHandles$Lookup} instance to use.
         * @return An appropriate class injector.
         */
        public static UsingLookup of(Object lookup) {
            if (!DISPATCHER.isAlive()) {
                throw new IllegalStateException("The current VM does not support class definition via method handle lookups");
            } else if (!JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().isInstance(lookup)) {
                throw new IllegalArgumentException("Not a method handle lookup: " + lookup);
            } else if ((DISPATCHER.lookupModes(lookup) & PACKAGE_LOOKUP) == 0) {
                throw new IllegalArgumentException("Lookup does not imply package-access: " + lookup);
            }
            return new UsingLookup(DISPATCHER.dropLookupMode(lookup, Opcodes.ACC_PRIVATE));
        }

        /**
         * Checks if the current VM is capable of defining classes using a method handle lookup.
         *
         * @return {@code true} if the current VM is capable of defining classes using a lookup.
         */
        public static boolean isAvailable() {
            return DISPATCHER.isAlive();
        }

        /**
         * Returns the lookup type this injector is based upon.
         *
         * @return The lookup type.
         */
        public Class<?> lookupType() {
            return DISPATCHER.lookupType(lookup);
        }

        @Override
        public Map<TypeDescription, Class<?>> inject(Map<? extends TypeDescription, byte[]> types) {
            Map<TypeDescription, Class<?>> loaded = new HashMap<TypeDescription, Class<?>>();
            for (Map.Entry<? extends TypeDescription, byte[]> entry : types.entrySet()) {
                if (!entry.getKey().isSamePackage(new TypeDescription.ForLoadedType(lookupType()))) {
                    throw new IllegalArgumentException(entry.getKey() + " must be defined in the same package as " + lookup);
                }
                loaded.put(entry.getKey(), DISPATCHER.defineClass(lookup, entry.getValue()));
            }
            return loaded;
        }

        /**
         * A dispatcher for interacting with a method handle lookup.
         */
        protected interface Dispatcher {

            /**
             * Indicates if this dispatcher is available on the current VM.
             *
             * @return {@code true} if this dispatcher is alive.
             */
            boolean isAlive();

            /**
             * Returns the lookup type for a given method handle lookup.
             *
             * @param lookup The lookup instance.
             * @return The lookup type.
             */
            Class<?> lookupType(Object lookup);

            /**
             * Returns a lookup objects lookup types.
             *
             * @param lookup The lookup instance.
             * @return The modifiers indicating the instance's lookup modes.
             */
            int lookupModes(Object lookup);

            /**
             * Drops a given lookup mode from a lookup instance.
             *
             * @param lookup The lookup instance.
             * @param mode   The modes to drop.
             * @return A new lookup instance where the modes were dropped.
             */
            Object dropLookupMode(Object lookup, int mode);

            /**
             * Defines a class.
             *
             * @param lookup               The {@code java.lang.invoke.MethodHandles$Lookup} instance to use.
             * @param binaryRepresentation The defined class's binary representation.
             * @return The defined class.
             */
            Class<?> defineClass(Object lookup, byte[] binaryRepresentation);

            /**
             * An action for defining a dispatcher.
             */
            enum Creator implements PrivilegedAction<Dispatcher> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
                public Dispatcher run() {
                    try {
                        Class<?> lookup = JavaType.METHOD_HANDLES_LOOKUP.load();
                        return new Dispatcher.ForJava9CapableVm(lookup.getMethod("lookupClass"),
                                lookup.getMethod("lookupModes"),
                                lookup.getMethod("dropLookupMode", int.class),
                                lookup.getMethod("defineClass", byte[].class));
                    } catch (Exception ignored) {
                        return Dispatcher.ForLegacyVm.INSTANCE;
                    }
                }
            }

            /**
             * A dispatcher for a legacy VM that does not support class definition via method handles.
             */
            enum ForLegacyVm implements Dispatcher {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public boolean isAlive() {
                    return false;
                }

                @Override
                public Class<?> lookupType(Object lookup) {
                    throw new IllegalStateException("Cannot dispatch method for java.lang.invoke.MethodHandles$Lookup");
                }

                @Override
                public int lookupModes(Object lookup) {
                    throw new IllegalStateException("Cannot dispatch method for java.lang.invoke.MethodHandles$Lookup");
                }

                @Override
                public Object dropLookupMode(Object lookup, int mode) {
                    throw new IllegalStateException("Cannot dispatch method for java.lang.invoke.MethodHandles$Lookup");
                }

                @Override
                public Class<?> defineClass(Object lookup, byte[] binaryRepresentation) {
                    throw new IllegalStateException("Cannot dispatch method for java.lang.invoke.MethodHandles$Lookup");
                }
            }

            /**
             * A dispatcher for a Java 9 capable VM that supports class definition via method handles.
             */
            @EqualsAndHashCode
            class ForJava9CapableVm implements Dispatcher {

                /**
                 * The {@code java.lang.invoke.MethodHandles$Lookup#lookupClass} method.
                 */
                private final Method lookupClass;

                /**
                 * The {@code java.lang.invoke.MethodHandles$Lookup#lookupModes} method.
                 */
                private final Method lookupModes;

                /**
                 * The {@code java.lang.invoke.MethodHandles$Lookup#dropLookupMode} method.
                 */
                private final Method dropLookupMode;

                /**
                 * The {@code java.lang.invoke.MethodHandles$Lookup#defineClass} method.
                 */
                private final Method defineClass;

                /**
                 * Creates a new dispatcher for a Java 9 capable VM.
                 *
                 * @param lookupClass    The {@code java.lang.invoke.MethodHandles$Lookup#lookupClass} method.
                 * @param lookupModes    The {@code java.lang.invoke.MethodHandles$Lookup#lookupModes} method.
                 * @param dropLookupMode The {@code java.lang.invoke.MethodHandles$Lookup#dropLookupMode} method.
                 * @param defineClass    The {@code java.lang.invoke.MethodHandles$Lookup#defineClass} method.
                 */
                protected ForJava9CapableVm(Method lookupClass, Method lookupModes, Method dropLookupMode, Method defineClass) {
                    this.lookupClass = lookupClass;
                    this.lookupModes = lookupModes;
                    this.defineClass = defineClass;
                    this.dropLookupMode = dropLookupMode;
                }

                @Override
                public boolean isAlive() {
                    return true;
                }

                @Override
                public Class<?> lookupType(Object lookup) {
                    try {
                        return (Class<?>) lookupClass.invoke(lookup);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandles$Lookup#lookupClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandles$Lookup#lookupClass", exception.getCause());
                    }
                }

                @Override
                public int lookupModes(Object lookup) {
                    try {
                        return (Integer) lookupModes.invoke(lookup);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandles$Lookup#lookupModes", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandles$Lookup#lookupModes", exception.getCause());
                    }
                }

                @Override
                public Object dropLookupMode(Object lookup, int mode) {
                    try {
                        return dropLookupMode.invoke(lookup, mode);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandles$Lookup#lookupModes", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandles$Lookup#lookupModes", exception.getCause());
                    }
                }

                @Override
                public Class<?> defineClass(Object lookup, byte[] binaryRepresentation) {
                    try {
                        return (Class<?>) defineClass.invoke(lookup, (Object) binaryRepresentation);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandles$Lookup#defineClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandles$Lookup#defineClass", exception.getCause());
                    }
                }
            }
        }
    }

    /**
     * A class injector that uses {@code sun.misc.Unsafe} to inject classes.
     */
    @EqualsAndHashCode
    class UsingUnsafe implements ClassInjector {

        /**
         * The dispatcher to use.
         */
        private static final Dispatcher.Initializable DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

        /**
         * A lock for the bootstrap loader when injecting code.
         */
        private static final Object BOOTSTRAP_LOADER_LOCK = new Object();

        /**
         * The class loader to inject classes into or {@code null} for the bootstrap loader.
         */
        private final ClassLoader classLoader;

        /**
         * The protection domain to use or {@code null} for no protection domain.
         */
        private final ProtectionDomain protectionDomain;

        /**
         * Creates a new unsafe injector for the given class loader with a default protection domain.
         *
         * @param classLoader The class loader to inject classes into or {@code null} for the bootstrap loader.
         */
        public UsingUnsafe(ClassLoader classLoader) {
            this(classLoader, ClassLoadingStrategy.NO_PROTECTION_DOMAIN);
        }

        /**
         * Creates a new unsafe injector for the given class loader with a default protection domain.
         *
         * @param classLoader      The class loader to inject classes into or {@code null} for the bootstrap loader.
         * @param protectionDomain The protection domain to use or {@code null} for no protection domain.
         */
        public UsingUnsafe(ClassLoader classLoader, ProtectionDomain protectionDomain) {
            this.classLoader = classLoader;
            this.protectionDomain = protectionDomain;
        }

        /**
         * Checks if unsafe class injection is available on the current VM.
         *
         * @return {@code true} if unsafe class injection is available on the current VM.
         */
        public static boolean isAvailable() {
            return DISPATCHER.isAvailable();
        }

        /**
         * Returns an unsafe class injector for the bootstrap class loader.
         *
         * @return A class injector for the bootstrap loader.
         */
        public static ClassInjector ofBootstrapLoader() {
            return new UsingUnsafe(ClassLoadingStrategy.BOOTSTRAP_LOADER);
        }

        /**
         * Returns an unsafe class injector for the class path.
         *
         * @return A class injector for the system class loader.
         */
        public static ClassInjector ofClassPath() {
            return new UsingUnsafe(ClassLoader.getSystemClassLoader());
        }

        @Override
        public Map<TypeDescription, Class<?>> inject(Map<? extends TypeDescription, byte[]> types) {
            Dispatcher dispatcher = DISPATCHER.initialize();
            Map<TypeDescription, Class<?>> loaded = new HashMap<TypeDescription, Class<?>>();
            synchronized (classLoader == null
                    ? BOOTSTRAP_LOADER_LOCK
                    : classLoader) {
                for (Map.Entry<? extends TypeDescription, byte[]> entry : types.entrySet()) {
                    try {
                        loaded.put(entry.getKey(), Class.forName(entry.getKey().getName(), false, classLoader));
                    } catch (ClassNotFoundException ignored) {
                        loaded.put(entry.getKey(), dispatcher.defineClass(classLoader, entry.getKey().getName(), entry.getValue(), protectionDomain));
                    }
                }
            }
            return loaded;
        }

        /**
         * A dispatcher for using {@code sun.misc.Unsafe}.
         */
        interface Dispatcher {

            /**
             * Defines a class.
             *
             * @param classLoader          The class loader to inject the class into.
             * @param name                 The type's name.
             * @param binaryRepresentation The type's binary representation.
             * @param protectionDomain     The type's protection domain.
             * @return The defined class.
             */
            Class<?> defineClass(ClassLoader classLoader, String name, byte[] binaryRepresentation, ProtectionDomain protectionDomain);

            /**
             * A class injection dispatcher that is not yet initialized.
             */
            interface Initializable {

                /**
                 * Checks if unsafe class injection is available on the current VM.
                 *
                 * @return {@code true} if unsafe class injection is available.
                 */
                boolean isAvailable();

                /**
                 * Initializes the dispatcher.
                 *
                 * @return The initialized dispatcher.
                 */
                Dispatcher initialize();
            }

            /**
             * A privileged action for creating a dispatcher.
             */
            enum CreationAction implements PrivilegedAction<Initializable> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
                public Initializable run() {
                    try {
                        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
                        return new Enabled(unsafe.getDeclaredField("theUnsafe"), unsafe.getMethod("defineClass",
                                String.class,
                                byte[].class,
                                int.class,
                                int.class,
                                ClassLoader.class,
                                ProtectionDomain.class));
                    } catch (Exception exception) {
                        return new Disabled(exception);
                    }
                }
            }

            /**
             * An enabled dispatcher.
             */
            @EqualsAndHashCode
            class Enabled implements Dispatcher, Initializable {

                /**
                 * A field containing {@code sun.misc.Unsafe}.
                 */
                private final Field theUnsafe;

                /**
                 * The {@code sun.misc.Unsafe#defineClass} method.
                 */
                private final Method defineClass;

                /**
                 * Creates an enabled dispatcher.
                 *
                 * @param theUnsafe   A field containing {@code sun.misc.Unsafe}.
                 * @param defineClass The {@code sun.misc.Unsafe#defineClass} method.
                 */
                protected Enabled(Field theUnsafe, Method defineClass) {
                    this.theUnsafe = theUnsafe;
                    this.defineClass = defineClass;
                }

                @Override
                public boolean isAvailable() {
                    return true;
                }

                @Override
                @SuppressFBWarnings(value = "DP_DO_INSIDE_DO_PRIVILEGED", justification = "Privilege is explicit caller responsibility")
                public Dispatcher initialize() {
                    theUnsafe.setAccessible(true);
                    return this;
                }

                @Override
                public Class<?> defineClass(ClassLoader classLoader, String name, byte[] binaryRepresentation, ProtectionDomain protectionDomain) {
                    try {
                        return (Class<?>) defineClass.invoke(theUnsafe.get(null),
                                name,
                                binaryRepresentation,
                                0,
                                binaryRepresentation.length,
                                classLoader,
                                protectionDomain);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access Unsafe::defineClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking Unsafe::defineClass", exception.getCause());
                    }
                }
            }

            /**
             * A disabled dispatcher.
             */
            @EqualsAndHashCode
            class Disabled implements Initializable {

                /**
                 * The exception causing this dispatcher's creation.
                 */
                private final Exception exception;

                /**
                 * Creates a disabled dispatcher.
                 *
                 * @param exception The exception causing this dispatcher's creation.
                 */
                protected Disabled(Exception exception) {
                    this.exception = exception;
                }

                @Override
                public boolean isAvailable() {
                    return false;
                }

                @Override
                public Dispatcher initialize() {
                    throw new IllegalStateException("Could not find sun.misc.Unsafe", exception);
                }
            }
        }
    }

    /**
     * A class injector using a {@link java.lang.instrument.Instrumentation} to append to either the boot classpath
     * or the system class path.
     */
    @EqualsAndHashCode
    class UsingInstrumentation implements ClassInjector {

        /**
         * The jar file name extension.
         */
        private static final String JAR = "jar";

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
            File jarFile = new File(folder, JAR + randomString.nextString() + "." + JAR);
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
        }
    }
}
