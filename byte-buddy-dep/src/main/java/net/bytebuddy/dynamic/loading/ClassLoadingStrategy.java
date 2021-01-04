/*
 * Copyright 2014 - Present Rafael Winterhalter
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

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A strategy for loading a collection of types.
 *
 * @param <T> The least specific type of class loader this strategy can apply to.
 */
public interface ClassLoadingStrategy<T extends ClassLoader> {

    /**
     * A type-safe constant representing the bootstrap class loader which is represented by {@code null} within Java.
     */
    ClassLoader BOOTSTRAP_LOADER = null;

    /**
     * An undefined protection domain.
     */
    ProtectionDomain NO_PROTECTION_DOMAIN = null;

    /**
     * Loads a given collection of classes given their binary representation.
     *
     * @param classLoader The class loader to used for loading the classes.
     * @param types       Byte array representations of the types to be loaded mapped by their descriptions,
     *                    where an iteration order defines an order in which they are supposed to be loaded,
     *                    if relevant.
     * @return A collection of the loaded classes which will be initialized in the iteration order of the
     * returned collection.
     */
    Map<TypeDescription, Class<?>> load(T classLoader, Map<TypeDescription, byte[]> types);

    /**
     * This class contains implementations of default class loading strategies.
     */
    enum Default implements Configurable<ClassLoader> {

        /**
         * This strategy creates a new {@link net.bytebuddy.dynamic.loading.ByteArrayClassLoader} with the given
         * class loader as its parent. The byte array class loader is aware of a any dynamically created type and can
         * natively load the given classes. This allows to load classes with cyclic load-time dependencies since the
         * byte array class loader is queried on each encountered unknown class. Due to the encapsulation of the
         * classes that were loaded by a byte array class loader, this strategy will lead to the unloading of these
         * classes once this class loader, its classes or any instances of these classes become unreachable.
         */
        WRAPPER(new WrappingDispatcher(ByteArrayClassLoader.PersistenceHandler.LATENT, WrappingDispatcher.PARENT_FIRST)),

        /**
         * The strategy is identical to {@link ClassLoadingStrategy.Default#WRAPPER} but exposes
         * the byte arrays that represent a class by {@link java.lang.ClassLoader#getResourceAsStream(String)}. For
         * this purpose, all class files are persisted as byte arrays withing the wrapping class loader.
         */
        WRAPPER_PERSISTENT(new WrappingDispatcher(ByteArrayClassLoader.PersistenceHandler.MANIFEST, WrappingDispatcher.PARENT_FIRST)),

        /**
         * <p>
         * The child-first class loading strategy is a modified version of the
         * {@link ClassLoadingStrategy.Default#WRAPPER} where the dynamic types are given
         * priority over any types of a parent class loader with the same name.
         * </p>
         * <p>
         * <b>Important</b>: This does <i>not</i> replace a type of the same name, but it makes the type invisible by
         * the reach of this class loader.
         * </p>
         */
        CHILD_FIRST(new WrappingDispatcher(ByteArrayClassLoader.PersistenceHandler.LATENT, WrappingDispatcher.CHILD_FIRST)),

        /**
         * The strategy is identical to {@link ClassLoadingStrategy.Default#CHILD_FIRST} but
         * exposes the byte arrays that represent a class by {@link java.lang.ClassLoader#getResourceAsStream(String)}.
         * For this purpose, all class files are persisted as byte arrays withing the wrapping class loader.
         */
        CHILD_FIRST_PERSISTENT(new WrappingDispatcher(ByteArrayClassLoader.PersistenceHandler.MANIFEST, WrappingDispatcher.CHILD_FIRST)),

        /**
         * <p>
         * This strategy does not create a new class loader but injects all classes into the given {@link java.lang.ClassLoader}
         * by reflective access. This prevents the loading of classes with cyclic load-time dependencies but avoids the
         * creation of an additional class loader. The advantage of this strategy is that the loaded classes will have
         * package-private access to other classes within their package of the class loader into which they are
         * injected what is not permitted when the wrapper class loader is used. This strategy is implemented using a
         * {@link net.bytebuddy.dynamic.loading.ClassInjector.UsingReflection}. Note that this strategy usually yields
         * a better runtime performance.
         * </p>
         * <p>
         * <b>Important</b>: Class injection requires access to JVM internal methods that are sealed by security managers and the
         * Java Platform module system. Since Java 11, access to these methods is no longer feasible unless those packages
         * are explicitly opened.
         * </p>
         * <p>
         * <b>Note</b>: This class loader does not define packages for injected classes by default. Therefore, calls to
         * {@link Class#getPackage()} might return {@code null}. Packages are only defined manually by a class loader prior to
         * Java 9.
         * </p>
         */
        INJECTION(new InjectionDispatcher());

        /**
         * The default behavior when attempting to load a type that was already loaded.
         */
        private static final boolean DEFAULT_FORBID_EXISTING = true;

        /**
         * The dispatcher to be used when loading a class.
         */
        private final Configurable<ClassLoader> dispatcher;

        /**
         * Creates a new default class loading strategy.
         *
         * @param dispatcher The dispatcher to be used when loading a class.
         */
        Default(Configurable<ClassLoader> dispatcher) {
            this.dispatcher = dispatcher;
        }

        /**
         * {@inheritDoc}
         */
        public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
            return dispatcher.load(classLoader, types);
        }

        /**
         * {@inheritDoc}
         */
        public Configurable<ClassLoader> with(ProtectionDomain protectionDomain) {
            return dispatcher.with(protectionDomain);
        }

        /**
         * {@inheritDoc}
         */
        public Configurable<ClassLoader> with(PackageDefinitionStrategy packageDefinitionStrategy) {
            return dispatcher.with(packageDefinitionStrategy);
        }

        /**
         * {@inheritDoc}
         */
        public Configurable<ClassLoader> allowExistingTypes() {
            return dispatcher.allowExistingTypes();
        }

        /**
         * {@inheritDoc}
         */
        public Configurable<ClassLoader> opened() {
            return dispatcher.opened();
        }

        /**
         * <p>
         * A class loading strategy which applies a class loader injection while applying a given {@link java.security.ProtectionDomain} on class injection.
         * </p>
         * <p>
         * <b>Important</b>: Class injection requires access to JVM internal methods that are sealed by security managers and the
         * Java Platform module system. Since Java 11, access to these methods is no longer feasible unless those packages
         * are explicitly opened.
         * </p>
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class InjectionDispatcher implements ClassLoadingStrategy.Configurable<ClassLoader> {

            /**
             * The protection domain to apply or {@code null} if no protection domain is set.
             */
            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
            private final ProtectionDomain protectionDomain;

            /**
             * The package definer to be used for querying information on package information.
             */
            private final PackageDefinitionStrategy packageDefinitionStrategy;

            /**
             * Determines if an exception should be thrown when attempting to load a type that already exists.
             */
            private final boolean forbidExisting;

            /**
             * Creates a new injection dispatcher.
             */
            protected InjectionDispatcher() {
                this(NO_PROTECTION_DOMAIN, PackageDefinitionStrategy.NoOp.INSTANCE, DEFAULT_FORBID_EXISTING);
            }

            /**
             * Creates a new injection dispatcher.
             *
             * @param protectionDomain          The protection domain to apply or {@code null} if no protection domain is set.
             * @param packageDefinitionStrategy The package definer to be used for querying information on package information.
             * @param forbidExisting            Determines if an exception should be thrown when attempting to load a type that already exists.
             */
            private InjectionDispatcher(ProtectionDomain protectionDomain,
                                        PackageDefinitionStrategy packageDefinitionStrategy,
                                        boolean forbidExisting) {
                this.protectionDomain = protectionDomain;
                this.packageDefinitionStrategy = packageDefinitionStrategy;
                this.forbidExisting = forbidExisting;
            }

            /**
             * {@inheritDoc}
             */
            public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
                return new ClassInjector.UsingReflection(classLoader,
                        protectionDomain,
                        packageDefinitionStrategy,
                        forbidExisting).inject(types);
            }

            /**
             * {@inheritDoc}
             */
            public Configurable<ClassLoader> with(ProtectionDomain protectionDomain) {
                return new InjectionDispatcher(protectionDomain, packageDefinitionStrategy, forbidExisting);
            }

            /**
             * {@inheritDoc}
             */
            public Configurable<ClassLoader> with(PackageDefinitionStrategy packageDefinitionStrategy) {
                return new InjectionDispatcher(protectionDomain, packageDefinitionStrategy, forbidExisting);
            }

            /**
             * {@inheritDoc}
             */
            public Configurable<ClassLoader> allowExistingTypes() {
                return new InjectionDispatcher(protectionDomain, packageDefinitionStrategy, false);
            }

            /**
             * {@inheritDoc}
             */
            public Configurable<ClassLoader> opened() {
                return this;
            }
        }

        /**
         * A class loading strategy which creates a wrapping class loader while applying a given
         * {@link java.security.ProtectionDomain} on class loading.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class WrappingDispatcher implements ClassLoadingStrategy.Configurable<ClassLoader> {

            /**
             * Indicates that a child first loading strategy should be attempted.
             */
            private static final boolean CHILD_FIRST = true;

            /**
             * Indicates that a parent first loading strategy should be attempted.
             */
            private static final boolean PARENT_FIRST = false;

            /**
             * The protection domain to apply or {@code null} if no protection domain is set.
             */
            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
            private final ProtectionDomain protectionDomain;

            /**
             * The persistence handler to apply.
             */
            private final ByteArrayClassLoader.PersistenceHandler persistenceHandler;

            /**
             * The package definer to be used for querying information on package information.
             */
            private final PackageDefinitionStrategy packageDefinitionStrategy;

            /**
             * {@code true} if the created class loader should apply child-first semantics.
             */
            private final boolean childFirst;

            /**
             * Determines if an exception should be thrown when attempting to load a type that already exists.
             */
            private final boolean forbidExisting;

            /**
             * {@code true} if the class loader should be sealed.
             */
            private final boolean sealed;

            /**
             * Creates a new wrapping dispatcher with a default protection domain and a default access control context.
             *
             * @param persistenceHandler The persistence handler to apply.
             * @param childFirst         {@code true} if the created class loader should apply child-first semantics.
             */
            protected WrappingDispatcher(ByteArrayClassLoader.PersistenceHandler persistenceHandler, boolean childFirst) {
                this(NO_PROTECTION_DOMAIN,
                        PackageDefinitionStrategy.Trivial.INSTANCE,
                        persistenceHandler,
                        childFirst,
                        DEFAULT_FORBID_EXISTING,
                        true);
            }

            /**
             * Creates a new protection domain specific class loading wrapper.
             *
             * @param protectionDomain          The protection domain to apply or {@code null} if no protection domain is set.
             * @param packageDefinitionStrategy The package definer to be used for querying information on package information.
             * @param persistenceHandler        The persistence handler to apply.
             * @param childFirst                {@code true} if the created class loader should apply child-first semantics.
             * @param forbidExisting            Determines if an exception should be thrown when attempting to load a type that already exists.
             * @param sealed                    {@code true} if the class loader should be sealed.
             */
            private WrappingDispatcher(ProtectionDomain protectionDomain,
                                       PackageDefinitionStrategy packageDefinitionStrategy,
                                       ByteArrayClassLoader.PersistenceHandler persistenceHandler,
                                       boolean childFirst,
                                       boolean forbidExisting,
                                       boolean sealed) {
                this.protectionDomain = protectionDomain;
                this.packageDefinitionStrategy = packageDefinitionStrategy;
                this.persistenceHandler = persistenceHandler;
                this.childFirst = childFirst;
                this.forbidExisting = forbidExisting;
                this.sealed = sealed;
            }

            /**
             * {@inheritDoc}
             */
            public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
                return childFirst
                        ? ByteArrayClassLoader.ChildFirst.load(classLoader, types, protectionDomain, persistenceHandler, packageDefinitionStrategy, forbidExisting, sealed)
                        : ByteArrayClassLoader.load(classLoader, types, protectionDomain, persistenceHandler, packageDefinitionStrategy, forbidExisting, sealed);
            }

            /**
             * {@inheritDoc}
             */
            public Configurable<ClassLoader> with(ProtectionDomain protectionDomain) {
                return new WrappingDispatcher(protectionDomain, packageDefinitionStrategy, persistenceHandler, childFirst, forbidExisting, sealed);
            }

            /**
             * {@inheritDoc}
             */
            public Configurable<ClassLoader> with(PackageDefinitionStrategy packageDefinitionStrategy) {
                return new WrappingDispatcher(protectionDomain, packageDefinitionStrategy, persistenceHandler, childFirst, forbidExisting, sealed);
            }

            /**
             * {@inheritDoc}
             */
            public Configurable<ClassLoader> allowExistingTypes() {
                return new WrappingDispatcher(protectionDomain, packageDefinitionStrategy, persistenceHandler, childFirst, false, sealed);
            }

            /**
             * {@inheritDoc}
             */
            public Configurable<ClassLoader> opened() {
                return new WrappingDispatcher(protectionDomain, packageDefinitionStrategy, persistenceHandler, childFirst, forbidExisting, false);
            }
        }
    }

    /**
     * A {@link ClassLoadingStrategy} that allows configuring the strategy's behavior.
     *
     * @param <S> The least specific type of class loader this strategy can apply to.
     */
    interface Configurable<S extends ClassLoader> extends ClassLoadingStrategy<S> {

        /**
         * Overrides the implicitly set default {@link java.security.ProtectionDomain} with an explicit one.
         *
         * @param protectionDomain The protection domain to apply or {@code null} if no protection domain is set.
         * @return This class loading strategy with an explicitly set {@link java.security.ProtectionDomain}.
         */
        Configurable<S> with(ProtectionDomain protectionDomain);

        /**
         * Defines the supplied package definition strategy to be used for defining packages.
         *
         * @param packageDefinitionStrategy The package definer to be used.
         * @return A version of this class loading strategy that applies the supplied package definition strategy.
         */
        Configurable<S> with(PackageDefinitionStrategy packageDefinitionStrategy);

        /**
         * Determines if this class loading strategy should not throw an exception when attempting to load a class that
         * was already loaded. In this case, the already loaded class is used instead of the generated class.
         *
         * @return A version of this class loading strategy that does not throw an exception when a class is already loaded.
         */
        Configurable<S> allowExistingTypes();

        /**
         * With an opened class loading strategy, it is assured that types can be added to the class loader, either by
         * indirect injection using this strategy or by creating a class loader that explicitly supports injection.
         *
         * @return A version of this class loading strategy that opens for future injections into a class loader.
         */
        Configurable<S> opened();
    }

    /**
     * A class loading strategy that uses a {@code java.lang.invoke.MethodHandles$Lookup} instance for defining types.
     * A lookup instance can define types only in the same class loader and in the same package as the type within which
     * it was created. The supplied lookup must have package privileges, i.e. it must not be a public lookup.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class UsingLookup implements ClassLoadingStrategy<ClassLoader> {

        /**
         * The class injector to use.
         */
        private final ClassInjector classInjector;

        /**
         * Creates a new class loading strategy that uses a lookup type.
         *
         * @param classInjector The class injector to use.
         */
        protected UsingLookup(ClassInjector classInjector) {
            this.classInjector = classInjector;
        }

        /**
         * Creates a new class loading strategy that uses a {@code java.lang.invoke.MethodHandles$Lookup} instance.
         *
         * @param lookup The lookup instance to use for defining new types.
         * @return A suitable class loading strategy.
         */
        public static ClassLoadingStrategy<ClassLoader> of(Object lookup) {
            return new UsingLookup(ClassInjector.UsingLookup.of(lookup));
        }

        /**
         * Resolves a class loading strategy using a lookup if available on the current JVM. If the current JVM supports method handles
         * lookups, a lookup instance will be used. Alternatively, unsafe class definition is used, if supported. If neither strategy is
         * supported, an exception is thrown. A common use case would be calling this method as in
         * {@code ClassLoadingStrategy.UsingLookup.withFallback(MethodHandles::lookup)}. Note that the invocation of
         * {@code MethodHandles.lookup()} is call site sensitive such that it cannot be invoked within Byte Buddy what requires this
         * external invocation.
         *
         * @param lookup A resolver for a lookup instance if the current JVM allows for lookup-based class injection.
         * @return An appropriate class loading strategy for the current JVM that uses a method handles lookup if available.
         */
        public static ClassLoadingStrategy<ClassLoader> withFallback(Callable<?> lookup) {
            if (ClassInjector.UsingLookup.isAvailable()) {
                try {
                    return of(lookup.call());
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            } else if (ClassInjector.UsingUnsafe.isAvailable()) {
                return new ClassLoadingStrategy.ForUnsafeInjection();
            } else {
                throw new IllegalStateException("Neither lookup or unsafe class injection is available");
            }
        }

        /**
         * {@inheritDoc}
         */
        public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
            return classInjector.inject(types);
        }
    }

    /**
     * A class loading strategy which allows class injection into the bootstrap class loader if
     * appropriate.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForBootstrapInjection implements ClassLoadingStrategy<ClassLoader> {

        /**
         * The instrumentation to use.
         */
        private final Instrumentation instrumentation;

        /**
         * The folder to save jar files in.
         */
        private final File folder;

        /**
         * Creates a new injector which is capable of injecting classes into the bootstrap class loader.
         *
         * @param instrumentation The instrumentation to use.
         * @param folder          The folder to save jar files in.
         */
        public ForBootstrapInjection(Instrumentation instrumentation, File folder) {
            this.instrumentation = instrumentation;
            this.folder = folder;
        }

        /**
         * {@inheritDoc}
         */
        public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
            ClassInjector classInjector = classLoader == null
                    ? ClassInjector.UsingInstrumentation.of(folder, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation)
                    : new ClassInjector.UsingReflection(classLoader);
            return classInjector.inject(types);
        }
    }

    /**
     * A class loading strategy that injects a class using {@code sun.misc.Unsafe}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForUnsafeInjection implements ClassLoadingStrategy<ClassLoader> {

        /**
         * The protection domain to use or {@code null} if no protection domain is set.
         */
        @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
        private final ProtectionDomain protectionDomain;

        /**
         * Creates a new class loading strategy for unsafe injection with a default protection domain.
         */
        public ForUnsafeInjection() {
            this(NO_PROTECTION_DOMAIN);
        }

        /**
         * Creates a new class loading strategy for unsafe injection.
         *
         * @param protectionDomain The protection domain to use or {@code null} if no protection domain is set.
         */
        public ForUnsafeInjection(ProtectionDomain protectionDomain) {
            this.protectionDomain = protectionDomain;
        }

        /**
         * {@inheritDoc}
         */
        public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
            return new ClassInjector.UsingUnsafe(classLoader, protectionDomain).inject(types);
        }
    }
}
