package net.bytebuddy.dynamic.loading;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.type.TypeDescription;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Map;

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
         * <b>Important</b>: This class loader does not define packages for injected classes by default. Therefore, calls to
         * {@link Class#getPackage()} might return {@code null}. Packages are only defined
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

        @Override
        public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
            return dispatcher.load(classLoader, types);
        }

        @Override
        public Configurable<ClassLoader> with(ProtectionDomain protectionDomain) {
            return dispatcher.with(protectionDomain);
        }

        @Override
        public Configurable<ClassLoader> with(PackageDefinitionStrategy packageDefinitionStrategy) {
            return dispatcher.with(packageDefinitionStrategy);
        }

        @Override
        public Configurable<ClassLoader> allowExistingTypes() {
            return dispatcher.allowExistingTypes();
        }

        /**
         * A class loading strategy which applies a class loader injection while applying a given
         * {@link java.security.ProtectionDomain} on class injection.
         */
        @EqualsAndHashCode
        protected static class InjectionDispatcher implements ClassLoadingStrategy.Configurable<ClassLoader> {

            /**
             * The protection domain to apply.
             */
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
             * @param protectionDomain          The protection domain to apply.
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

            @Override
            public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
                return new ClassInjector.UsingReflection(classLoader,
                        protectionDomain,
                        packageDefinitionStrategy,
                        forbidExisting).inject(types);
            }

            @Override
            public Configurable<ClassLoader> with(ProtectionDomain protectionDomain) {
                return new InjectionDispatcher(protectionDomain, packageDefinitionStrategy, forbidExisting);
            }

            @Override
            public Configurable<ClassLoader> with(PackageDefinitionStrategy packageDefinitionStrategy) {
                return new InjectionDispatcher(protectionDomain, packageDefinitionStrategy, forbidExisting);
            }

            @Override
            public Configurable<ClassLoader> allowExistingTypes() {
                return new InjectionDispatcher(protectionDomain, packageDefinitionStrategy, false);
            }
        }

        /**
         * A class loading strategy which creates a wrapping class loader while applying a given
         * {@link java.security.ProtectionDomain} on class loading.
         */
        @EqualsAndHashCode
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
             * The protection domain to apply.
             */
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
                        DEFAULT_FORBID_EXISTING);
            }

            /**
             * Creates a new protection domain specific class loading wrapper.
             *
             * @param protectionDomain          The protection domain to apply.
             * @param packageDefinitionStrategy The package definer to be used for querying information on package information.
             * @param persistenceHandler        The persistence handler to apply.
             * @param childFirst                {@code true} if the created class loader should apply child-first semantics.
             * @param forbidExisting            Determines if an exception should be thrown when attempting to load a type that already exists.
             */
            private WrappingDispatcher(ProtectionDomain protectionDomain,
                                       PackageDefinitionStrategy packageDefinitionStrategy,
                                       ByteArrayClassLoader.PersistenceHandler persistenceHandler,
                                       boolean childFirst,
                                       boolean forbidExisting) {
                this.protectionDomain = protectionDomain;
                this.packageDefinitionStrategy = packageDefinitionStrategy;
                this.persistenceHandler = persistenceHandler;
                this.childFirst = childFirst;
                this.forbidExisting = forbidExisting;
            }

            @Override
            public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
                return ByteArrayClassLoader.load(classLoader,
                        types,
                        protectionDomain,
                        persistenceHandler,
                        packageDefinitionStrategy,
                        childFirst,
                        forbidExisting);
            }

            @Override
            public Configurable<ClassLoader> with(ProtectionDomain protectionDomain) {
                return new WrappingDispatcher(protectionDomain, packageDefinitionStrategy, persistenceHandler, childFirst, forbidExisting);
            }

            @Override
            public Configurable<ClassLoader> with(PackageDefinitionStrategy packageDefinitionStrategy) {
                return new WrappingDispatcher(protectionDomain, packageDefinitionStrategy, persistenceHandler, childFirst, forbidExisting);
            }

            @Override
            public Configurable<ClassLoader> allowExistingTypes() {
                return new InjectionDispatcher(protectionDomain, packageDefinitionStrategy, false);
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
         * @param protectionDomain The protection domain to apply.
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
    }

    /**
     * A class loading strategy which allows class injection into the bootstrap class loader if
     * appropriate.
     */
    @EqualsAndHashCode
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

        @Override
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
    @EqualsAndHashCode
    class ForUnsafeInjection implements ClassLoadingStrategy<ClassLoader> {

        /**
         * The protection domain to use.
         */
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
         * @param protectionDomain The protection domain to use.
         */
        public ForUnsafeInjection(ProtectionDomain protectionDomain) {
            this.protectionDomain = protectionDomain;
        }

        @Override
        public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
            return new ClassInjector.UsingUnsafe(classLoader, protectionDomain).inject(types);
        }
    }
}
