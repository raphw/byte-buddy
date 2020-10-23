/*
 * Copyright 2014 - 2020 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.dynamic.loading;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.MemberRemoval;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.JavaType;
import net.bytebuddy.utility.RandomString;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.net.URL;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.named;

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
     * A permission for the {@code suppressAccessChecks} permission.
     */
    Permission SUPPRESS_ACCESS_CHECKS = new ReflectPermission("suppressAccessChecks");

    /**
     * Determines the default behavior for type injections when a type is already loaded.
     */
    boolean ALLOW_EXISTING_TYPES = false;

    /**
     * Indicates if this class injector is available on the current VM.
     *
     * @return {@code true} if this injector is available on the current VM.
     */
    boolean isAlive();

    /**
     * Injects the given types into the represented class loader.
     *
     * @param types The types to load via injection.
     * @return The loaded types that were passed as arguments.
     */
    Map<TypeDescription, Class<?>> inject(Map<? extends TypeDescription, byte[]> types);

    /**
     * Injects the given types into the represented class loader using a mapping from name to binary representation.
     *
     * @param types The types to load via injection.
     * @return The loaded types that were passed as arguments.
     */
    Map<String, Class<?>> injectRaw(Map<? extends String, byte[]> types);

    /**
     * An abstract base implementation of a class injector.
     */
    abstract class AbstractBase implements ClassInjector {

        /**
         * {@inheritDoc}
         */
        public Map<TypeDescription, Class<?>> inject(Map<? extends TypeDescription, byte[]> types) {
            Map<String, byte[]> binaryRepresentations = new LinkedHashMap<String, byte[]>();
            for (Map.Entry<? extends TypeDescription, byte[]> entry : types.entrySet()) {
                binaryRepresentations.put(entry.getKey().getName(), entry.getValue());
            }
            Map<String, Class<?>> loadedTypes = injectRaw(binaryRepresentations);
            Map<TypeDescription, Class<?>> result = new LinkedHashMap<TypeDescription, Class<?>>();
            for (TypeDescription typeDescription : types.keySet()) {
                result.put(typeDescription, loadedTypes.get(typeDescription.getName()));
            }
            return result;
        }
    }

    /**
     * A class injector that uses reflective method calls.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class UsingReflection extends AbstractBase {

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
        @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
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
         * {@inheritDoc}
         */
        public boolean isAlive() {
            return isAvailable();
        }

        /**
         * {@inheritDoc}
         */
        public Map<String, Class<?>> injectRaw(Map<? extends String, byte[]> types) {
            Dispatcher dispatcher = DISPATCHER.initialize();
            Map<String, Class<?>> result = new HashMap<String, Class<?>>();
            for (Map.Entry<? extends String, byte[]> entry : types.entrySet()) {
                synchronized (dispatcher.getClassLoadingLock(classLoader, entry.getKey())) {
                    Class<?> type = dispatcher.findClass(classLoader, entry.getKey());
                    if (type == null) {
                        int packageIndex = entry.getKey().lastIndexOf('.');
                        if (packageIndex != -1) {
                            String packageName = entry.getKey().substring(0, packageIndex);
                            PackageDefinitionStrategy.Definition definition = packageDefinitionStrategy.define(classLoader, packageName, entry.getKey());
                            if (definition.isDefined()) {
                                Package definedPackage = dispatcher.getDefinedPackage(classLoader, packageName);
                                if (definedPackage == null) {
                                    try {
                                        dispatcher.definePackage(classLoader,
                                                packageName,
                                                definition.getSpecificationTitle(),
                                                definition.getSpecificationVersion(),
                                                definition.getSpecificationVendor(),
                                                definition.getImplementationTitle(),
                                                definition.getImplementationVersion(),
                                                definition.getImplementationVendor(),
                                                definition.getSealBase());
                                    } catch (IllegalStateException exception) {
                                        // Custom classloaders may call getPackage (instead of getDefinedPackage) from
                                        // within definePackage, which can cause the package to be defined in an
                                        // ancestor classloader or find a previously defined one from an ancestor. In
                                        // this case definePackage will also throw since it considers that package
                                        // already loaded and will not allow to define it directly in this classloader.
                                        // To make sure this is the case, call getPackage instead of getDefinedPackage
                                        // here and verify that we actually have a compatible package defined in an
                                        // ancestor classloader. This issue is known to happen on WLS14+JDK11.
                                        definedPackage = dispatcher.getPackage(classLoader, packageName);
                                        if (definedPackage == null) {
                                            throw exception;
                                        } else if (!definition.isCompatibleTo(definedPackage)) {
                                            throw new SecurityException("Sealing violation for package " + packageName + " (getPackage fallback)");
                                        }
                                    }
                                } else if (!definition.isCompatibleTo(definedPackage)) {
                                    throw new SecurityException("Sealing violation for package " + packageName);
                                }
                            }
                        }
                        type = dispatcher.defineClass(classLoader, entry.getKey(), entry.getValue(), protectionDomain);
                    } else if (forbidExisting) {
                        throw new IllegalStateException("Cannot inject already loaded type: " + type);
                    }
                    result.put(entry.getKey(), type);
                }
            }
            return result;
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
             * Looks up a package from a class loader. If the operation is not supported, falls back to {@link #getPackage(ClassLoader, String)}
             *
             * @param classLoader The class loader to query.
             * @param name        The binary name of the package.
             * @return The package for the given name as defined by the provided class loader or {@code null} if no such package exists.
             */
            Package getDefinedPackage(ClassLoader classLoader, String name);

            /**
             * Looks up a package from a class loader or its ancestor.
             *
             * @param classLoader The class loader to query.
             * @param name        The binary name of the package.
             * @return The package for the given name as defined by the provided class loader or its ancestor, or {@code null} if no such package exists.
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

                /**
                 * Represents an unsuccessfully loaded method lookup.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Unavailable implements Dispatcher, Initializable {

                    /**
                     * The reason why this dispatcher is not available.
                     */
                    private final String message;

                    /**
                     * Creates a new faulty reflection store.
                     *
                     * @param message The reason why this dispatcher is not available.
                     */
                    protected Unavailable(String message) {
                        this.message = message;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public boolean isAvailable() {
                        return false;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Dispatcher initialize() {
                        return this;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Object getClassLoadingLock(ClassLoader classLoader, String name) {
                        return classLoader;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<?> findClass(ClassLoader classLoader, String name) {
                        try {
                            return classLoader.loadClass(name);
                        } catch (ClassNotFoundException ignored) {
                            return UNDEFINED;
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<?> defineClass(ClassLoader classLoader, String name, byte[] binaryRepresentation, ProtectionDomain protectionDomain) {
                        throw new UnsupportedOperationException("Cannot define class using reflection: " + message);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Package getDefinedPackage(ClassLoader classLoader, String name) {
                        throw new UnsupportedOperationException("Cannot get defined package using reflection: " + message);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Package getPackage(ClassLoader classLoader, String name) {
                        throw new UnsupportedOperationException("Cannot get package using reflection: " + message);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Package definePackage(ClassLoader classLoader,
                                                 String name,
                                                 String specificationTitle,
                                                 String specificationVersion,
                                                 String specificationVendor,
                                                 String implementationTitle,
                                                 String implementationVersion,
                                                 String implementationVendor,
                                                 URL sealBase) {
                        throw new UnsupportedOperationException("Cannot define package using injection: " + message);
                    }
                }
            }

            /**
             * A creation action for a dispatcher.
             */
            enum CreationAction implements PrivilegedAction<Initializable> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
                public Initializable run() {
                    try {
                        if (JavaModule.isSupported()) {
                            return UsingUnsafe.isAvailable()
                                    ? UsingUnsafeInjection.make()
                                    : UsingUnsafeOverride.make();
                        } else {
                            return Direct.make();
                        }
                    } catch (InvocationTargetException exception) {
                        return new Initializable.Unavailable(exception.getCause().getMessage());
                    } catch (Exception exception) {
                        return new Initializable.Unavailable(exception.getMessage());
                    }
                }
            }

            /**
             * A class injection dispatcher that is using reflection on the {@link ClassLoader} methods.
             */
            @HashCodeAndEqualsPlugin.Enhance
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
                 * An instance of {@link ClassLoader#getDefinedPackage(String)}. May be {@code null}.
                 */
                protected final Method getDefinedPackage;

                /**
                 * An instance of {@link ClassLoader#getPackage(String)}.
                 */
                protected final Method getPackage;

                /**
                 * An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                 */
                protected final Method definePackage;

                /**
                 * Creates a new direct injection dispatcher.
                 *
                 * @param findLoadedClass   An instance of {@link ClassLoader#findLoadedClass(String)}.
                 * @param defineClass       An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                 * @param getDefinedPackage An instance of {@link ClassLoader#getDefinedPackage(String)}. May be {@code null}.
                 * @param getPackage        An instance of {@link ClassLoader#getPackage(String)}.
                 * @param definePackage     An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                 */
                protected Direct(Method findLoadedClass,
                                 Method defineClass,
                                 Method getDefinedPackage,
                                 Method getPackage,
                                 Method definePackage) {
                    this.findLoadedClass = findLoadedClass;
                    this.defineClass = defineClass;
                    this.getDefinedPackage = getDefinedPackage;
                    this.getPackage = getPackage;
                    this.definePackage = definePackage;
                }

                /**
                 * Creates a direct dispatcher.
                 *
                 * @return A direct dispatcher for class injection.
                 * @throws Exception If the creation is impossible.
                 */
                @SuppressFBWarnings(value = "DP_DO_INSIDE_DO_PRIVILEGED", justification = "Privilege is explicit caller responsibility")
                protected static Initializable make() throws Exception {
                    Method getDefinedPackage;
                    if (JavaModule.isSupported()) { // Avoid accidental lookup of method with same name in Java 8 J9 VM.
                        try {
                            getDefinedPackage = ClassLoader.class.getMethod("getDefinedPackage", String.class);
                            getDefinedPackage.setAccessible(true);
                        } catch (NoSuchMethodException ignored) {
                            getDefinedPackage = null;
                        }
                    } else {
                        getDefinedPackage = null;
                    }
                    Method getPackage = ClassLoader.class.getDeclaredMethod("getPackage", String.class);
                    getPackage.setAccessible(true);
                    Method findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
                    findLoadedClass.setAccessible(true);
                    Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass",
                            String.class,
                            byte[].class,
                            int.class,
                            int.class,
                            ProtectionDomain.class);
                    defineClass.setAccessible(true);
                    Method definePackage = ClassLoader.class.getDeclaredMethod("definePackage",
                            String.class,
                            String.class,
                            String.class,
                            String.class,
                            String.class,
                            String.class,
                            String.class,
                            URL.class);
                    definePackage.setAccessible(true);
                    try {
                        Method getClassLoadingLock = ClassLoader.class.getDeclaredMethod("getClassLoadingLock", String.class);
                        getClassLoadingLock.setAccessible(true);
                        return new ForJava7CapableVm(findLoadedClass,
                                defineClass,
                                getDefinedPackage,
                                getPackage,
                                definePackage,
                                getClassLoadingLock);
                    } catch (NoSuchMethodException ignored) {
                        return new ForLegacyVm(findLoadedClass, defineClass, getDefinedPackage, getPackage, definePackage);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isAvailable() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher initialize() {
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null) {
                        try {
                            securityManager.checkPermission(SUPPRESS_ACCESS_CHECKS);
                        } catch (Exception exception) {
                            return new Dispatcher.Unavailable(exception.getMessage());
                        }
                    }
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> findClass(ClassLoader classLoader, String name) {
                    try {
                        return (Class<?>) findLoadedClass.invoke(classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access java.lang.ClassLoader#findClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.ClassLoader#findClass", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
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
                public Package getDefinedPackage(ClassLoader classLoader, String name) {
                    if (getDefinedPackage == null) {
                        return getPackage(classLoader, name);
                    }
                    try {
                        return (Package) getDefinedPackage.invoke(classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access java.lang.ClassLoader#getDefinedPackage", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.ClassLoader#getDefinedPackage", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Package getPackage(ClassLoader classLoader, String name) {
                    try {
                        return (Package) getPackage.invoke(classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access java.lang.ClassLoader#getPackage", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.ClassLoader#getPackage", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
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

                /**
                 * A resolved class dispatcher for a class injector on a VM running at least Java 7.
                 */
                @HashCodeAndEqualsPlugin.Enhance
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
                     * @param getDefinedPackage   An instance of {@link ClassLoader#getDefinedPackage(String)}. May be {@code null}.
                     * @param getPackage          An instance of {@link ClassLoader#getPackage(String)}.
                     * @param definePackage       An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                     */
                    protected ForJava7CapableVm(Method findLoadedClass,
                                                Method defineClass,
                                                Method getDefinedPackage,
                                                Method getPackage,
                                                Method definePackage,
                                                Method getClassLoadingLock) {
                        super(findLoadedClass, defineClass, getDefinedPackage, getPackage, definePackage);
                        this.getClassLoadingLock = getClassLoadingLock;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Object getClassLoadingLock(ClassLoader classLoader, String name) {
                        try {
                            return getClassLoadingLock.invoke(classLoader, name);
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException("Could not access java.lang.ClassLoader#getClassLoadingLock", exception);
                        } catch (InvocationTargetException exception) {
                            throw new IllegalStateException("Error invoking java.lang.ClassLoader#getClassLoadingLock", exception.getCause());
                        }
                    }
                }

                /**
                 * A resolved class dispatcher for a class injector prior to Java 7.
                 */
                protected static class ForLegacyVm extends Direct {

                    /**
                     * Creates a new resolved reflection store for a VM prior to Java 8.
                     *
                     * @param findLoadedClass   An instance of {@link ClassLoader#findLoadedClass(String)}.
                     * @param defineClass       An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                     * @param getDefinedPackage An instance of {@link ClassLoader#getDefinedPackage(String)}. May be {@code null}.
                     * @param getPackage        An instance of {@link ClassLoader#getPackage(String)}.
                     * @param definePackage     An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                     */
                    protected ForLegacyVm(Method findLoadedClass,
                                          Method defineClass,
                                          Method getDefinedPackage,
                                          Method getPackage,
                                          Method definePackage) {
                        super(findLoadedClass, defineClass, getDefinedPackage, getPackage, definePackage);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Object getClassLoadingLock(ClassLoader classLoader, String name) {
                        return classLoader;
                    }
                }
            }

            /**
             * An indirect dispatcher that uses a redirection accessor class that was injected into the bootstrap class loader.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class UsingUnsafeInjection implements Dispatcher, Initializable {

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
                 * The accessor method for using {@link ClassLoader#getDefinedPackage(String)}. May be {@code null}.
                 */
                private final Method getDefinedPackage;

                /**
                 * The accessor method for using {@link ClassLoader#getPackage(String)}.
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
                 * Creates a new class loading injection dispatcher using an unsafe injected dispatcher.
                 *
                 * @param accessor            An instance of the accessor class that is required for using it's intentionally non-static methods.
                 * @param findLoadedClass     An instance of {@link ClassLoader#findLoadedClass(String)}.
                 * @param defineClass         An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                 * @param getDefinedPackage   An instance of {@link ClassLoader#getDefinedPackage(String)}. May be {@code null}.
                 * @param getPackage          An instance of {@link ClassLoader#getPackage(String)}.
                 * @param definePackage       An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                 * @param getClassLoadingLock The accessor method for using {@code ClassLoader#getClassLoadingLock(String)} or returning the
                 *                            supplied {@link ClassLoader} if this method does not exist on the current VM.
                 */
                protected UsingUnsafeInjection(Object accessor,
                                               Method findLoadedClass,
                                               Method defineClass,
                                               Method getDefinedPackage,
                                               Method getPackage,
                                               Method definePackage,
                                               Method getClassLoadingLock) {
                    this.accessor = accessor;
                    this.findLoadedClass = findLoadedClass;
                    this.defineClass = defineClass;
                    this.getDefinedPackage = getDefinedPackage;
                    this.getPackage = getPackage;
                    this.definePackage = definePackage;
                    this.getClassLoadingLock = getClassLoadingLock;
                }

                /**
                 * Creates an indirect dispatcher.
                 *
                 * @return An indirect dispatcher for class creation.
                 * @throws Exception If the dispatcher cannot be created.
                 */
                @SuppressFBWarnings(value = "DP_DO_INSIDE_DO_PRIVILEGED", justification = "Privilege is explicit caller responsibility")
                protected static Initializable make() throws Exception {
                    if (Boolean.getBoolean(UsingUnsafe.SAFE_PROPERTY)) {
                        return new Initializable.Unavailable("Use of Unsafe was disabled by system property");
                    }
                    Class<?> unsafe = Class.forName("sun.misc.Unsafe");
                    Field theUnsafe = unsafe.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    Object unsafeInstance = theUnsafe.get(null);
                    Method getDefinedPackage;
                    if (JavaModule.isSupported()) { // Avoid accidental lookup of method with same name in Java 8 J9 VM.
                        try {
                            getDefinedPackage = ClassLoader.class.getDeclaredMethod("getDefinedPackage", String.class);
                        } catch (NoSuchMethodException ignored) {
                            getDefinedPackage = null;
                        }
                    } else {
                        getDefinedPackage = null;
                    }
                    DynamicType.Builder<?> builder = new ByteBuddy()
                            .with(TypeValidation.DISABLED)
                            .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                            .name(ClassLoader.class.getName() + "$ByteBuddyAccessor$" + RandomString.make())
                            .defineMethod("findLoadedClass", Class.class, Visibility.PUBLIC)
                            .withParameters(ClassLoader.class, String.class)
                            .intercept(MethodCall.invoke(ClassLoader.class
                                    .getDeclaredMethod("findLoadedClass", String.class))
                                    .onArgument(0)
                                    .withArgument(1))
                            .defineMethod("defineClass", Class.class, Visibility.PUBLIC)
                            .withParameters(ClassLoader.class, String.class, byte[].class, int.class, int.class,
                                    ProtectionDomain.class)
                            .intercept(MethodCall.invoke(ClassLoader.class
                                    .getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class))
                                    .onArgument(0)
                                    .withArgument(1, 2, 3, 4, 5))
                            .defineMethod("getPackage", Package.class, Visibility.PUBLIC)
                            .withParameters(ClassLoader.class, String.class)
                            .intercept(MethodCall.invoke(ClassLoader.class
                                    .getDeclaredMethod("getPackage", String.class))
                                    .onArgument(0)
                                    .withArgument(1))
                            .defineMethod("definePackage", Package.class, Visibility.PUBLIC)
                            .withParameters(ClassLoader.class, String.class, String.class, String.class, String.class,
                                    String.class, String.class, String.class, URL.class)
                            .intercept(MethodCall.invoke(ClassLoader.class
                                    .getDeclaredMethod("definePackage", String.class, String.class, String.class, String.class, String.class, String.class, String.class, URL.class))
                                    .onArgument(0)
                                    .withArgument(1, 2, 3, 4, 5, 6, 7, 8));
                    if (getDefinedPackage != null) {
                        builder = builder
                            .defineMethod("getDefinedPackage", Package.class, Visibility.PUBLIC)
                            .withParameters(ClassLoader.class, String.class)
                            .intercept(MethodCall.invoke(getDefinedPackage)
                                .onArgument(0)
                                .withArgument(1));
                    }
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
                    Class<?> type = builder.make()
                            .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, new ClassLoadingStrategy.ForUnsafeInjection())
                            .getLoaded();
                    return new UsingUnsafeInjection(
                            unsafe.getMethod("allocateInstance", Class.class).invoke(unsafeInstance, type),
                            type.getMethod("findLoadedClass", ClassLoader.class, String.class),
                            type.getMethod("defineClass", ClassLoader.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class),
                            getDefinedPackage != null ? type.getMethod("getDefinedPackage", ClassLoader.class, String.class) : null,
                            type.getMethod("getPackage", ClassLoader.class, String.class),
                            type.getMethod("definePackage", ClassLoader.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, URL.class),
                            type.getMethod("getClassLoadingLock", ClassLoader.class, String.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isAvailable() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher initialize() {
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null) {
                        try {
                            securityManager.checkPermission(SUPPRESS_ACCESS_CHECKS);
                        } catch (Exception exception) {
                            return new Dispatcher.Unavailable(exception.getMessage());
                        }
                    }
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public Object getClassLoadingLock(ClassLoader classLoader, String name) {
                    try {
                        return getClassLoadingLock.invoke(accessor, classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access (accessor)::getClassLoadingLock", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking (accessor)::getClassLoadingLock", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> findClass(ClassLoader classLoader, String name) {
                    try {
                        return (Class<?>) findLoadedClass.invoke(accessor, classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access (accessor)::findLoadedClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking (accessor)::findLoadedClass", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> defineClass(ClassLoader classLoader, String name, byte[] binaryRepresentation, ProtectionDomain protectionDomain) {
                    try {
                        return (Class<?>) defineClass.invoke(accessor, classLoader, name, binaryRepresentation, 0, binaryRepresentation.length, protectionDomain);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access (accessor)::defineClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking (accessor)::defineClass", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Package getDefinedPackage(ClassLoader classLoader, String name) {
                    if (getDefinedPackage == null) {
                        return getPackage(classLoader, name);
                    }
                    try {
                        return (Package) getDefinedPackage.invoke(accessor, classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access (accessor)::getDefinedPackage", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking (accessor)::getDefinedPackage", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Package getPackage(ClassLoader classLoader, String name) {
                    try {
                        return (Package) getPackage.invoke(accessor, classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access (accessor)::getPackage", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking (accessor)::getPackage", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
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
             * A dispatcher implementation that uses {@code sun.misc.Unsafe#putBoolean} to set the {@link AccessibleObject} field
             * for making methods accessible.
             */
            abstract class UsingUnsafeOverride implements Dispatcher, Initializable {

                /**
                 * An instance of {@link ClassLoader#findLoadedClass(String)}.
                 */
                protected final Method findLoadedClass;

                /**
                 * An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                 */
                protected final Method defineClass;

                /**
                 * An instance of {@link ClassLoader#getDefinedPackage(String)}. May be {@code null}.
                 */
                protected final Method getDefinedPackage;

                /**
                 * An instance of {@link ClassLoader#getPackage(String)}.
                 */
                protected final Method getPackage;

                /**
                 * An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                 */
                protected final Method definePackage;

                /**
                 * Creates a new unsafe field injecting injection dispatcher.
                 *
                 * @param findLoadedClass   An instance of {@link ClassLoader#findLoadedClass(String)}.
                 * @param defineClass       An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                 * @param getDefinedPackage An instance of {@link ClassLoader#getDefinedPackage(String)}. May be {@code null}.
                 * @param getPackage        An instance of {@link ClassLoader#getPackage(String)}.
                 * @param definePackage     An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                 */
                protected UsingUnsafeOverride(Method findLoadedClass,
                                              Method defineClass,
                                              Method getDefinedPackage,
                                              Method getPackage,
                                              Method definePackage) {
                    this.findLoadedClass = findLoadedClass;
                    this.defineClass = defineClass;
                    this.getDefinedPackage = getDefinedPackage;
                    this.getPackage = getPackage;
                    this.definePackage = definePackage;
                }

                /**
                 * Creates a new initializable class injector using an unsafe field injection.
                 *
                 * @return An appropriate initializable.
                 * @throws Exception If the injector cannot be created.
                 */
                @SuppressFBWarnings(value = "DP_DO_INSIDE_DO_PRIVILEGED", justification = "Privilege is explicit caller responsibility")
                protected static Initializable make() throws Exception {
                    if (Boolean.getBoolean(UsingUnsafe.SAFE_PROPERTY)) {
                        return new Initializable.Unavailable("Use of Unsafe was disabled by system property");
                    }
                    Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
                    Field theUnsafe = unsafeType.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    Object unsafe = theUnsafe.get(null);
                    Field override;
                    try {
                        override = AccessibleObject.class.getDeclaredField("override");
                    } catch (NoSuchFieldException ignored) {
                        // Since Java 12, the override field is hidden from the reflection API. To circumvent this, we
                        // create a mirror class of AccessibleObject that defines the same fields and has the same field
                        // layout such that the override field will receive the same class offset. Doing so, we can write to
                        // the offset location and still set a value to it, despite it being hidden from the reflection API.
                        override = new ByteBuddy()
                                .redefine(AccessibleObject.class)
                                .name("net.bytebuddy.mirror." + AccessibleObject.class.getSimpleName())
                                .noNestMate()
                                .visit(new MemberRemoval().stripInvokables(any()))
                                .make()
                                .load(AccessibleObject.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                                .getLoaded()
                                .getDeclaredField("override");
                    }
                    long offset = (Long) unsafeType
                            .getMethod("objectFieldOffset", Field.class)
                            .invoke(unsafe, override);
                    Method putBoolean = unsafeType.getMethod("putBoolean", Object.class, long.class, boolean.class);
                    Method getDefinedPackage;
                    if (JavaModule.isSupported()) { // Avoid accidental lookup of method with same name in Java 8 J9 VM.
                        try {
                            getDefinedPackage = ClassLoader.class.getMethod("getDefinedPackage", String.class);
                            putBoolean.invoke(unsafe, getDefinedPackage, offset, true);
                        } catch (NoSuchMethodException ignored) {
                            getDefinedPackage = null;
                        }
                    } else {
                        getDefinedPackage = null;
                    }
                    Method getPackage = ClassLoader.class.getDeclaredMethod("getPackage", String.class);
                    putBoolean.invoke(unsafe, getPackage, offset, true);
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
                    putBoolean.invoke(unsafe, defineClass, offset, true);
                    putBoolean.invoke(unsafe, findLoadedClass, offset, true);
                    putBoolean.invoke(unsafe, definePackage, offset, true);
                    try {
                        Method getClassLoadingLock = ClassLoader.class.getDeclaredMethod("getClassLoadingLock", String.class);
                        putBoolean.invoke(unsafe, getClassLoadingLock, offset, true);
                        return new ForJava7CapableVm(findLoadedClass,
                                defineClass,
                                getDefinedPackage,
                                getPackage,
                                definePackage,
                                getClassLoadingLock);
                    } catch (NoSuchMethodException ignored) {
                        return new ForLegacyVm(findLoadedClass, defineClass, getDefinedPackage, getPackage, definePackage);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isAvailable() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher initialize() {
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null) {
                        try {
                            securityManager.checkPermission(SUPPRESS_ACCESS_CHECKS);
                        } catch (Exception exception) {
                            return new Dispatcher.Unavailable(exception.getMessage());
                        }
                    }
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> findClass(ClassLoader classLoader, String name) {
                    try {
                        return (Class<?>) findLoadedClass.invoke(classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access java.lang.ClassLoader#findClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.ClassLoader#findClass", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
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
                public Package getDefinedPackage(ClassLoader classLoader, String name) {
                    if (getDefinedPackage == null) {
                        return getPackage(classLoader, name);
                    }
                    try {
                        return (Package) getDefinedPackage.invoke(classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access java.lang.ClassLoader#getDefinedPackage", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.ClassLoader#getDefinedPackage", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Package getPackage(ClassLoader classLoader, String name) {
                    try {
                        return (Package) getPackage.invoke(classLoader, name);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access java.lang.ClassLoader#getPackage", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.ClassLoader#getPackage", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
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

                /**
                 * A resolved class dispatcher using unsafe field injection for a class injector on a VM running at least Java 7.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected static class ForJava7CapableVm extends UsingUnsafeOverride {

                    /**
                     * An instance of {@code ClassLoader#getClassLoadingLock(String)}.
                     */
                    private final Method getClassLoadingLock;

                    /**
                     * Creates a new resolved class injector using unsafe field injection for a VM running at least Java 7.
                     *
                     * @param getClassLoadingLock An instance of {@code ClassLoader#getClassLoadingLock(String)}.
                     * @param findLoadedClass     An instance of {@link ClassLoader#findLoadedClass(String)}.
                     * @param defineClass         An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                     * @param getDefinedPackage   An instance of {@link ClassLoader#getDefinedPackage(String)}. May be {@code null}.
                     * @param getPackage          An instance of {@link ClassLoader#getPackage(String)}.
                     * @param definePackage       An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                     */
                    protected ForJava7CapableVm(Method findLoadedClass,
                                                Method defineClass,
                                                Method getDefinedPackage,
                                                Method getPackage,
                                                Method definePackage,
                                                Method getClassLoadingLock) {
                        super(findLoadedClass, defineClass, getDefinedPackage, getPackage, definePackage);
                        this.getClassLoadingLock = getClassLoadingLock;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Object getClassLoadingLock(ClassLoader classLoader, String name) {
                        try {
                            return getClassLoadingLock.invoke(classLoader, name);
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException("Could not access java.lang.ClassLoader#getClassLoadingLock", exception);
                        } catch (InvocationTargetException exception) {
                            throw new IllegalStateException("Error invoking java.lang.ClassLoader#getClassLoadingLock", exception.getCause());
                        }
                    }
                }

                /**
                 * A resolved class dispatcher using unsafe field injection for a class injector prior to Java 7.
                 */
                protected static class ForLegacyVm extends UsingUnsafeOverride {

                    /**
                     * Creates a new resolved class injector using unsafe field injection for a VM prior to Java 7.
                     *
                     * @param findLoadedClass   An instance of {@link ClassLoader#findLoadedClass(String)}.
                     * @param defineClass       An instance of {@link ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)}.
                     * @param getDefinedPackage An instance of {@link ClassLoader#getDefinedPackage(String)}. May be {@code null}.
                     * @param getPackage        An instance of {@link ClassLoader#getPackage(String)}.
                     * @param definePackage     An instance of {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}.
                     */
                    protected ForLegacyVm(Method findLoadedClass,
                                          Method defineClass,
                                          Method getDefinedPackage,
                                          Method getPackage,
                                          Method definePackage) {
                        super(findLoadedClass, defineClass, getDefinedPackage, getPackage, definePackage);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Object getClassLoadingLock(ClassLoader classLoader, String name) {
                        return classLoader;
                    }
                }
            }

            /**
             * Represents an unsuccessfully loaded method lookup.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Unavailable implements Dispatcher {

                /**
                 * The error message being displayed.
                 */
                private final String message;

                /**
                 * Creates a dispatcher for a VM that does not support reflective injection.
                 *
                 * @param message The error message being displayed.
                 */
                protected Unavailable(String message) {
                    this.message = message;
                }

                /**
                 * {@inheritDoc}
                 */
                public Object getClassLoadingLock(ClassLoader classLoader, String name) {
                    return classLoader;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> findClass(ClassLoader classLoader, String name) {
                    try {
                        return classLoader.loadClass(name);
                    } catch (ClassNotFoundException ignored) {
                        return UNDEFINED;
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> defineClass(ClassLoader classLoader, String name, byte[] binaryRepresentation, ProtectionDomain protectionDomain) {
                    throw new UnsupportedOperationException("Cannot define class using reflection: " + message);
                }

                /**
                 * {@inheritDoc}
                 */
                public Package getDefinedPackage(ClassLoader classLoader, String name) {
                    throw new UnsupportedOperationException("Cannot get defined package using reflection: " + message);
                }

                /**
                 * {@inheritDoc}
                 */
                public Package getPackage(ClassLoader classLoader, String name) {
                    throw new UnsupportedOperationException("Cannot get package using reflection: " + message);
                }

                /**
                 * {@inheritDoc}
                 */
                public Package definePackage(ClassLoader classLoader,
                                             String name,
                                             String specificationTitle,
                                             String specificationVersion,
                                             String specificationVendor,
                                             String implementationTitle,
                                             String implementationVersion,
                                             String implementationVendor,
                                             URL sealBase) {
                    throw new UnsupportedOperationException("Cannot define package using injection: " + message);
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
    @HashCodeAndEqualsPlugin.Enhance
    class UsingLookup extends AbstractBase {

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
            } else if (!JavaType.METHOD_HANDLES_LOOKUP.isInstance(lookup)) {
                throw new IllegalArgumentException("Not a method handle lookup: " + lookup);
            } else if ((DISPATCHER.lookupModes(lookup) & PACKAGE_LOOKUP) == 0) {
                throw new IllegalArgumentException("Lookup does not imply package-access: " + lookup);
            }
            return new UsingLookup(lookup);
        }

        /**
         * Returns the lookup type this injector is based upon.
         *
         * @return The lookup type.
         */
        public Class<?> lookupType() {
            return DISPATCHER.lookupType(lookup);
        }

        /**
         * Resolves this injector to use the supplied type's scope.
         *
         * @param type The type to resolve the access scope for.
         * @return An new injector with the specified scope.
         */
        public UsingLookup in(Class<?> type) {
            return new UsingLookup(DISPATCHER.resolve(lookup, type));
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAlive() {
            return isAvailable();
        }

        /**
         * {@inheritDoc}
         */
        public Map<String, Class<?>> injectRaw(Map<? extends String, byte[]> types) {
            String expectedPackage = TypeDescription.ForLoadedType.of(lookupType()).getPackage().getName();
            Map<String, Class<?>> result = new HashMap<String, Class<?>>();
            for (Map.Entry<? extends String, byte[]> entry : types.entrySet()) {
                int index = entry.getKey().lastIndexOf('.');
                if (!expectedPackage.equals(index == -1 ? "" : entry.getKey().substring(0, index))) {
                    throw new IllegalArgumentException(entry.getKey() + " must be defined in the same package as " + lookup);
                }
                result.put(entry.getKey(), DISPATCHER.defineClass(lookup, entry.getValue()));
            }
            return result;
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
             * Resolves the supplied lookup instance's access scope for the supplied type.
             *
             * @param lookup The lookup to use.
             * @param type   The type to resolve the scope for.
             * @return An appropriate lookup instance.
             */
            Object resolve(Object lookup, Class<?> type);

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

                /**
                 * {@inheritDoc}
                 */
                @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
                public Dispatcher run() {
                    try {
                        Class<?> lookup = JavaType.METHOD_HANDLES_LOOKUP.load();
                        return new Dispatcher.ForJava9CapableVm(JavaType.METHOD_HANDLES.load().getMethod("privateLookupIn", Class.class, lookup),
                                lookup.getMethod("lookupClass"),
                                lookup.getMethod("lookupModes"),
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

                /**
                 * {@inheritDoc}
                 */
                public boolean isAlive() {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> lookupType(Object lookup) {
                    throw new IllegalStateException("Cannot dispatch method for java.lang.invoke.MethodHandles$Lookup");
                }

                /**
                 * {@inheritDoc}
                 */
                public int lookupModes(Object lookup) {
                    throw new IllegalStateException("Cannot dispatch method for java.lang.invoke.MethodHandles$Lookup");
                }

                /**
                 * {@inheritDoc}
                 */
                public Object resolve(Object lookup, Class<?> type) {
                    throw new IllegalStateException("Cannot dispatch method for java.lang.invoke.MethodHandles");
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> defineClass(Object lookup, byte[] binaryRepresentation) {
                    throw new IllegalStateException("Cannot dispatch method for java.lang.invoke.MethodHandles$Lookup");
                }
            }

            /**
             * A dispatcher for a Java 9 capable VM that supports class definition via method handles.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForJava9CapableVm implements Dispatcher {

                /**
                 * An empty array that can be used to indicate no arguments to avoid an allocation on a reflective call.
                 */
                private static final Object[] NO_ARGUMENTS = new Object[0];

                /**
                 * The {@code java.lang.invoke.MethodHandles$#privateLookupIn} method.
                 */
                private final Method privateLookupIn;

                /**
                 * The {@code java.lang.invoke.MethodHandles$Lookup#lookupClass} method.
                 */
                private final Method lookupClass;

                /**
                 * The {@code java.lang.invoke.MethodHandles$Lookup#lookupModes} method.
                 */
                private final Method lookupModes;

                /**
                 * The {@code java.lang.invoke.MethodHandles$Lookup#defineClass} method.
                 */
                private final Method defineClass;

                /**
                 * Creates a new dispatcher for a Java 9 capable VM.
                 *
                 * @param privateLookupIn The {@code java.lang.invoke.MethodHandles$#privateLookupIn} method.
                 * @param lookupClass     The {@code java.lang.invoke.MethodHandles$Lookup#lookupClass} method.
                 * @param lookupModes     The {@code java.lang.invoke.MethodHandles$Lookup#lookupModes} method.
                 * @param defineClass     The {@code java.lang.invoke.MethodHandles$Lookup#defineClass} method.
                 */
                protected ForJava9CapableVm(Method privateLookupIn, Method lookupClass, Method lookupModes, Method defineClass) {
                    this.privateLookupIn = privateLookupIn;
                    this.lookupClass = lookupClass;
                    this.lookupModes = lookupModes;
                    this.defineClass = defineClass;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isAlive() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> lookupType(Object lookup) {
                    try {
                        return (Class<?>) lookupClass.invoke(lookup, NO_ARGUMENTS);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandles$Lookup#lookupClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandles$Lookup#lookupClass", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public int lookupModes(Object lookup) {
                    try {
                        return (Integer) lookupModes.invoke(lookup, NO_ARGUMENTS);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandles$Lookup#lookupModes", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandles$Lookup#lookupModes", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Object resolve(Object lookup, Class<?> type) {
                    try {
                        return privateLookupIn.invoke(null, type, lookup);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.invoke.MethodHandles#privateLookupIn", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.invoke.MethodHandles#privateLookupIn", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
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
    @HashCodeAndEqualsPlugin.Enhance
    class UsingUnsafe extends AbstractBase {

        /**
         * If this property is set, Byte Buddy does not make use of any {@code Unsafe} class.
         */
        public static final String SAFE_PROPERTY = "net.bytebuddy.safe";

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
        @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
        private final ClassLoader classLoader;

        /**
         * The protection domain to use or {@code null} for no protection domain.
         */
        @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
        private final ProtectionDomain protectionDomain;

        /**
         * The dispatcher to use.
         */
        private final Dispatcher.Initializable dispatcher;

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
            this(classLoader, protectionDomain, DISPATCHER);
        }

        /**
         * Creates a new unsafe injector for the given class loader with a default protection domain.
         *
         * @param classLoader      The class loader to inject classes into or {@code null} for the bootstrap loader.
         * @param protectionDomain The protection domain to use or {@code null} for no protection domain.
         * @param dispatcher       The dispatcher to use.
         */
        protected UsingUnsafe(ClassLoader classLoader, ProtectionDomain protectionDomain, Dispatcher.Initializable dispatcher) {
            this.classLoader = classLoader;
            this.protectionDomain = protectionDomain;
            this.dispatcher = dispatcher;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAlive() {
            return dispatcher.isAvailable();
        }

        /**
         * {@inheritDoc}
         */
        public Map<String, Class<?>> injectRaw(Map<? extends String, byte[]> types) {
            Dispatcher dispatcher = this.dispatcher.initialize();
            Map<String, Class<?>> result = new HashMap<String, Class<?>>();
            synchronized (classLoader == null
                    ? BOOTSTRAP_LOADER_LOCK
                    : classLoader) {
                for (Map.Entry<? extends String, byte[]> entry : types.entrySet()) {
                    try {
                        result.put(entry.getKey(), Class.forName(entry.getKey(), false, classLoader));
                    } catch (ClassNotFoundException ignored) {
                        result.put(entry.getKey(), dispatcher.defineClass(classLoader, entry.getKey(), entry.getValue(), protectionDomain));
                    }
                }
            }
            return result;
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
         * Returns an unsafe class injector for the system class loader.
         *
         * @return A class injector for the system class loader.
         */
        public static ClassInjector ofSystemLoader() {
            return new UsingUnsafe(ClassLoader.getSystemClassLoader());
        }

        /**
         * Returns an unsafe class injector for the platform class loader. For VMs of version 8 or older,
         * the extension class loader is represented instead.
         *
         * @return A class injector for the platform class loader.
         */
        public static ClassInjector ofPlatformLoader() {
            return new UsingUnsafe(ClassLoader.getSystemClassLoader().getParent());
        }

        /**
         * Returns an unsafe class injector for the boot class loader.
         *
         * @return A class injector for the boot loader.
         */
        public static ClassInjector ofBootLoader() {
            return new UsingUnsafe(ClassLoadingStrategy.BOOTSTRAP_LOADER);
        }

        /**
         * A dispatcher for using {@code sun.misc.Unsafe}.
         */
        protected interface Dispatcher {

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

                /**
                 * {@inheritDoc}
                 */
                @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
                public Initializable run() {
                    if (Boolean.getBoolean(SAFE_PROPERTY)) {
                        return new Unavailable("Use of Unsafe was disabled by system property");
                    }
                    try {
                        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
                        Field theUnsafe = unsafeType.getDeclaredField("theUnsafe");
                        theUnsafe.setAccessible(true);
                        Object unsafe = theUnsafe.get(null);
                        try {
                            Method defineClass = unsafeType.getMethod("defineClass",
                                    String.class,
                                    byte[].class,
                                    int.class,
                                    int.class,
                                    ClassLoader.class,
                                    ProtectionDomain.class);
                            defineClass.setAccessible(true);
                            return new Enabled(unsafe, defineClass);
                        } catch (Exception exception) {
                            try {
                                Field override;
                                try {
                                    override = AccessibleObject.class.getDeclaredField("override");
                                } catch (NoSuchFieldException ignored) {
                                    // Since Java 12, the override field is hidden from the reflection API. To circumvent this, we
                                    // create a mirror class of AccessibleObject that defines the same fields and has the same field
                                    // layout such that the override field will receive the same class offset. Doing so, we can write to
                                    // the offset location and still set a value to it, despite it being hidden from the reflection API.
                                    override = new ByteBuddy()
                                            .redefine(AccessibleObject.class)
                                            .name("net.bytebuddy.mirror." + AccessibleObject.class.getSimpleName())
                                            .noNestMate()
                                            .visit(new MemberRemoval().stripInvokables(any()))
                                            .make()
                                            .load(AccessibleObject.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                                            .getLoaded()
                                            .getDeclaredField("override");
                                }
                                long offset = (Long) unsafeType
                                        .getMethod("objectFieldOffset", Field.class)
                                        .invoke(unsafe, override);
                                Method putBoolean = unsafeType.getMethod("putBoolean", Object.class, long.class, boolean.class);
                                Class<?> internalUnsafe = Class.forName("jdk.internal.misc.Unsafe");
                                Field theUnsafeInternal = internalUnsafe.getDeclaredField("theUnsafe");
                                putBoolean.invoke(unsafe, theUnsafeInternal, offset, true);
                                Method defineClassInternal = internalUnsafe.getMethod("defineClass",
                                        String.class,
                                        byte[].class,
                                        int.class,
                                        int.class,
                                        ClassLoader.class,
                                        ProtectionDomain.class);
                                putBoolean.invoke(unsafe, defineClassInternal, offset, true);
                                return new Enabled(theUnsafeInternal.get(null), defineClassInternal);
                            } catch (Exception ignored) {
                                throw exception;
                            }
                        }
                    } catch (Exception exception) {
                        return new Unavailable(exception.getMessage());
                    }
                }
            }

            /**
             * An enabled dispatcher.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Enabled implements Dispatcher, Initializable {

                /**
                 * An instance of {@code sun.misc.Unsafe}.
                 */
                private final Object unsafe;

                /**
                 * The {@code sun.misc.Unsafe#defineClass} method.
                 */
                private final Method defineClass;

                /**
                 * Creates an enabled dispatcher.
                 *
                 * @param unsafe      An instance of {@code sun.misc.Unsafe}.
                 * @param defineClass The {@code sun.misc.Unsafe#defineClass} method.
                 */
                protected Enabled(Object unsafe, Method defineClass) {
                    this.unsafe = unsafe;
                    this.defineClass = defineClass;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isAvailable() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher initialize() {
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null) {
                        try {
                            securityManager.checkPermission(SUPPRESS_ACCESS_CHECKS);
                        } catch (Exception exception) {
                            return new Unavailable(exception.getMessage());
                        }
                    }
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
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
                        throw new IllegalStateException("Could not access Unsafe::defineClass", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking Unsafe::defineClass", exception.getCause());
                    }
                }
            }

            /**
             * A disabled dispatcher.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Unavailable implements Dispatcher, Initializable {

                /**
                 * The reason why this dispatcher is not available.
                 */
                private final String message;

                /**
                 * Creates a disabled dispatcher.
                 *
                 * @param message The reason why this dispatcher is not available.
                 */
                protected Unavailable(String message) {
                    this.message = message;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isAvailable() {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher initialize() {
                    throw new UnsupportedOperationException("Could not access Unsafe class: " + message);
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> defineClass(ClassLoader classLoader, String name, byte[] binaryRepresentation, ProtectionDomain protectionDomain) {
                    throw new UnsupportedOperationException("Could not access Unsafe class: " + message);
                }
            }
        }

        /**
         * A factory for creating a {@link ClassInjector} that uses {@code sun.misc.Unsafe} if available but attempts a fallback
         * to using {@code jdk.internal.misc.Unsafe} if the {@code jdk.internal} module is not resolved or unavailable.
         */
        @HashCodeAndEqualsPlugin.Enhance
        public static class Factory {

            /**
             * The dispatcher to use.
             */
            private final Dispatcher.Initializable dispatcher;

            /**
             * Creates a new factory for an unsafe class injector that uses Byte Buddy's privileges to
             * accessing {@code jdk.internal.misc.Unsafe} if available.
             */
            public Factory() {
                this(AccessResolver.Default.INSTANCE);
            }

            /**
             * Creates a new factory for an unsafe class injector.
             *
             * @param accessResolver The access resolver to use.
             */
            @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception is captured to trigger lazy error upon use.")
            public Factory(AccessResolver accessResolver) {
                Dispatcher.Initializable dispatcher;
                if (DISPATCHER.isAvailable()) {
                    dispatcher = DISPATCHER;
                } else {
                    try {
                        Class<?> unsafeType = Class.forName("jdk.internal.misc.Unsafe");
                        Field theUnsafe = unsafeType.getDeclaredField("theUnsafe");
                        accessResolver.apply(theUnsafe);
                        Object unsafe = theUnsafe.get(null);
                        Method defineClass = unsafeType.getMethod("defineClass",
                                String.class,
                                byte[].class,
                                int.class,
                                int.class,
                                ClassLoader.class,
                                ProtectionDomain.class);
                        accessResolver.apply(defineClass);
                        dispatcher = new Dispatcher.Enabled(unsafe, defineClass);
                    } catch (Exception exception) {
                        dispatcher = new Dispatcher.Unavailable(exception.getMessage());
                    }
                }
                this.dispatcher = dispatcher;
            }

            /**
             * Creates a new factory.
             *
             * @param dispatcher The dispatcher to use.
             */
            protected Factory(Dispatcher.Initializable dispatcher) {
                this.dispatcher = dispatcher;
            }

            /**
             * Resolves an injection strategy that uses unsafe injection if available and also attempts to open and use
             * {@code jdk.internal.misc.Unsafe} as a fallback. This method generates a new class and module for opening the
             * internal package to avoid its exposure to any non-trusted code.
             *
             * @param instrumentation The instrumentation instance to use for opening the internal package if required.
             * @return An appropriate injection strategy.
             */
            public static Factory resolve(Instrumentation instrumentation) {
                return resolve(instrumentation, false);
            }

            /**
             * Resolves an injection strategy that uses unsafe injection if available and also attempts to open and use
             * {@code jdk.internal.misc.Unsafe} as a fallback.
             *
             * @param instrumentation The instrumentation instance to use for opening the internal package if required.
             * @param local           {@code false} if a new class should in a separated class loader and module should be created for
             *                        opening the {@code jdk.internal.misc} package. This way, the internal package is not exposed to any
             *                        other classes within this class's module.
             * @return An appropriate injection strategy.
             */
            @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception intends to trigger disabled injection strategy.")
            public static Factory resolve(Instrumentation instrumentation, boolean local) {
                if (ClassInjector.UsingUnsafe.isAvailable() || !JavaModule.isSupported()) {
                    return new Factory();
                } else {
                    try {
                        Class<?> type = Class.forName("jdk.internal.misc.Unsafe");
                        PackageDescription packageDescription = new PackageDescription.ForLoadedPackage(type.getPackage());
                        JavaModule source = JavaModule.ofType(type), target = JavaModule.ofType(ClassInjector.UsingUnsafe.class);
                        if (source.isOpened(packageDescription, target)) {
                            return new Factory();
                        } else if (local) {
                            JavaModule module = JavaModule.ofType(AccessResolver.Default.class);
                            source.modify(instrumentation,
                                    Collections.singleton(module),
                                    Collections.<String, Set<JavaModule>>emptyMap(),
                                    Collections.singletonMap(packageDescription.getName(), Collections.singleton(module)),
                                    Collections.<Class<?>>emptySet(),
                                    Collections.<Class<?>, List<Class<?>>>emptyMap());
                            return new Factory();
                        } else {
                            Class<? extends AccessResolver> resolver = new ByteBuddy()
                                    .subclass(AccessResolver.class)
                                    .method(named("apply"))
                                    .intercept(MethodCall.invoke(AccessibleObject.class.getMethod("setAccessible", boolean.class))
                                            .onArgument(0)
                                            .with(true))
                                    .make()
                                    .load(AccessResolver.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER.with(AccessResolver.class.getProtectionDomain()))
                                    .getLoaded();
                            JavaModule module = JavaModule.ofType(resolver);
                            source.modify(instrumentation,
                                    Collections.singleton(module),
                                    Collections.<String, Set<JavaModule>>emptyMap(),
                                    Collections.singletonMap(packageDescription.getName(), Collections.singleton(module)),
                                    Collections.<Class<?>>emptySet(),
                                    Collections.<Class<?>, List<Class<?>>>emptyMap());
                            return new ClassInjector.UsingUnsafe.Factory(resolver.getConstructor().newInstance());
                        }
                    } catch (Exception exception) {
                        return new Factory(new Dispatcher.Unavailable(exception.getMessage()));
                    }
                }
            }

            /**
             * Returns {@code true} if this factory creates a valid dispatcher.
             *
             * @return {@code true} if this factory creates a valid dispatcher.
             */
            public boolean isAvailable() {
                return dispatcher.isAvailable();
            }

            /**
             * Creates a new class injector for the given class loader without a {@link ProtectionDomain}.
             *
             * @param classLoader The class loader to inject into or {@code null} to inject into the bootstrap loader.
             * @return An appropriate class injector.
             */
            public ClassInjector make(ClassLoader classLoader) {
                return make(classLoader, ClassLoadingStrategy.NO_PROTECTION_DOMAIN);
            }

            /**
             * Creates a new class injector for the given class loader and protection domain.
             *
             * @param classLoader      The class loader to inject into or {@code null} to inject into the bootstrap loader.
             * @param protectionDomain The protection domain to apply or {@code null} if no protection domain should be used.
             * @return An appropriate class injector.
             */
            public ClassInjector make(ClassLoader classLoader, ProtectionDomain protectionDomain) {
                return new UsingUnsafe(classLoader, protectionDomain, dispatcher);
            }

            /**
             * An access resolver that invokes {@link AccessibleObject#setAccessible(boolean)} to {@code true} in a given privilege scope.
             */
            public interface AccessResolver {

                /**
                 * Applies this access resolver.
                 *
                 * @param accessibleObject The accessible object to make accessible.
                 */
                void apply(AccessibleObject accessibleObject);

                /**
                 * A default access resolver that uses Byte Buddy's privilege scope.
                 */
                enum Default implements AccessResolver {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public void apply(AccessibleObject accessibleObject) {
                        accessibleObject.setAccessible(true);
                    }
                }
            }
        }
    }

    /**
     * A class injector using a {@link java.lang.instrument.Instrumentation} to append to either the boot classpath
     * or the system class path.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class UsingInstrumentation extends AbstractBase {

        /**
         * The jar file name extension.
         */
        private static final String JAR = "jar";

        /**
         * The class file extension.
         */
        private static final String CLASS_FILE_EXTENSION = ".class";

        /**
         * A dispatcher for interacting with the instrumentation API.
         */
        private static final Dispatcher DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

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
         * {@inheritDoc}
         */
        public boolean isAlive() {
            return isAvailable();
        }

        /**
         * {@inheritDoc}
         */
        public Map<String, Class<?>> injectRaw(Map<? extends String, byte[]> types) {
            File file = new File(folder, JAR + randomString.nextString() + "." + JAR);
            try {
                if (!file.createNewFile()) {
                    throw new IllegalStateException("Cannot create file " + file);
                }
                try {
                    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(file));
                    try {
                        for (Map.Entry<? extends String, byte[]> entry : types.entrySet()) {
                            jarOutputStream.putNextEntry(new JarEntry(entry.getKey().replace('.', '/') + CLASS_FILE_EXTENSION));
                            jarOutputStream.write(entry.getValue());
                        }
                    } finally {
                        jarOutputStream.close();
                    }
                    JarFile jarFile = new JarFile(file, false);
                    try {
                        target.inject(instrumentation, jarFile);
                    } finally {
                        jarFile.close();
                    }
                    Map<String, Class<?>> result = new HashMap<String, Class<?>>();
                    for (String name : types.keySet()) {
                        result.put(name, Class.forName(name, false, target.getClassLoader()));
                    }
                    return result;
                } finally {
                    if (!file.delete()) {
                        file.deleteOnExit();
                    }
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot write jar file to disk", exception);
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Cannot load injected class", exception);
            }
        }

        /**
         * Returns {@code true} if this class injector is available on this VM.
         *
         * @return {@code true} if this class injector is available on this VM.
         */
        public static boolean isAvailable() {
            return DISPATCHER.isAlive();
        }

        /**
         * A dispatcher to interact with the instrumentation API.
         */
        protected interface Dispatcher {

            /**
             * Returns {@code true} if this dispatcher is alive.
             *
             * @return {@code true} if this dispatcher is alive.
             */
            boolean isAlive();

            /**
             * Appends a jar file to the bootstrap class loader.
             *
             * @param instrumentation The instrumentation instance to interact with.
             * @param jarFile         The jar file to append.
             */
            void appendToBootstrapClassLoaderSearch(Instrumentation instrumentation, JarFile jarFile);

            /**
             * Appends a jar file to the system class loader.
             *
             * @param instrumentation The instrumentation instance to interact with.
             * @param jarFile         The jar file to append.
             */
            void appendToSystemClassLoaderSearch(Instrumentation instrumentation, JarFile jarFile);

            /**
             * An action to create a dispatcher for interacting with the instrumentation API.
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
                        Class<?> instrumentation = Class.forName("java.lang.instrument.Instrumentation");
                        return new ForJava6CapableVm(instrumentation.getMethod("appendToBootstrapClassLoaderSearch", JarFile.class),
                                instrumentation.getMethod("appendToSystemClassLoaderSearch", JarFile.class));
                    } catch (ClassNotFoundException ignored) {
                        return ForLegacyVm.INSTANCE;
                    } catch (NoSuchMethodException ignored) {
                        return ForLegacyVm.INSTANCE;
                    }
                }
            }

            /**
             * A dispatcher for a legacy VM that is not capable of appending jar files using instrumentation.
             */
            enum ForLegacyVm implements Dispatcher {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public boolean isAlive() {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public void appendToBootstrapClassLoaderSearch(Instrumentation instrumentation, JarFile jarFile) {
                    throw new UnsupportedOperationException("The current JVM does not support appending to the bootstrap loader");
                }

                /**
                 * {@inheritDoc}
                 */
                public void appendToSystemClassLoaderSearch(Instrumentation instrumentation, JarFile jarFile) {
                    throw new UnsupportedOperationException("The current JVM does not support appending to the system class loader");
                }
            }

            /**
             * A dispatcher for a VM that is capable of appending to the boot and system class loader.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForJava6CapableVm implements Dispatcher {

                /**
                 * The {@code Instrumentation#appendToBootstrapClassLoaderSearch} method.
                 */
                private final Method appendToBootstrapClassLoaderSearch;

                /**
                 * The {@code Instrumentation#appendToSystemClassLoaderSearch} method.
                 */
                private final Method appendToSystemClassLoaderSearch;

                /**
                 * Creates a new dispatcher for a Java 6 compatible VM.
                 *
                 * @param appendToBootstrapClassLoaderSearch The {@code Instrumentation#appendToBootstrapClassLoaderSearch} method.
                 * @param appendToSystemClassLoaderSearch    The {@code Instrumentation#appendToSystemClassLoaderSearch} method.
                 */
                protected ForJava6CapableVm(Method appendToBootstrapClassLoaderSearch, Method appendToSystemClassLoaderSearch) {
                    this.appendToBootstrapClassLoaderSearch = appendToBootstrapClassLoaderSearch;
                    this.appendToSystemClassLoaderSearch = appendToSystemClassLoaderSearch;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isAlive() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public void appendToBootstrapClassLoaderSearch(Instrumentation instrumentation, JarFile jarFile) {
                    try {
                        appendToBootstrapClassLoaderSearch.invoke(instrumentation, jarFile);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.instrument.Instrumentation#appendToBootstrapClassLoaderSearch", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.instrument.Instrumentation#appendToBootstrapClassLoaderSearch", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void appendToSystemClassLoaderSearch(Instrumentation instrumentation, JarFile jarFile) {
                    try {
                        appendToSystemClassLoaderSearch.invoke(instrumentation, jarFile);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.instrument.Instrumentation#appendToSystemClassLoaderSearch", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.instrument.Instrumentation#appendToSystemClassLoaderSearch", exception.getCause());
                    }
                }
            }
        }

        /**
         * A representation of the target to which Java classes should be appended to.
         */
        public enum Target {

            /**
             * Representation of the bootstrap class loader.
             */
            BOOTSTRAP(null) {
                @Override
                protected void inject(Instrumentation instrumentation, JarFile jarFile) {
                    DISPATCHER.appendToBootstrapClassLoaderSearch(instrumentation, jarFile);
                }
            },

            /**
             * Representation of the system class loader.
             */
            SYSTEM(ClassLoader.getSystemClassLoader()) {
                @Override
                protected void inject(Instrumentation instrumentation, JarFile jarFile) {
                    DISPATCHER.appendToSystemClassLoaderSearch(instrumentation, jarFile);
                }
            };

            /**
             * The class loader to load classes from.
             */
            private final ClassLoader classLoader;

            /**
             * Creates a new injection target.
             *
             * @param classLoader The class loader to load classes from.
             */
            Target(ClassLoader classLoader) {
                this.classLoader = classLoader;
            }

            /**
             * Returns the class loader to load classes from.
             *
             * @return The class loader to load classes from.
             */
            protected ClassLoader getClassLoader() {
                return classLoader;
            }

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
