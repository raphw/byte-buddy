package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.Map;

/**
 * A strategy for loading a collection of types.
 */
public interface ClassLoadingStrategy {

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
    Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types);

    /**
     * This class contains implementations of default class loading strategies.
     */
    enum Default implements Configurable {

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
         * A convenience reference that references the default protection domain which is {@code null}.
         */
        private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

        /**
         * The default behavior when attempting to load a type that was already loaded.
         */
        private static final boolean DEFAULT_FORBID_EXISTING = true;

        /**
         * The dispatcher to be used when loading a class.
         */
        private final Configurable dispatcher;

        /**
         * Creates a new default class loading strategy.
         *
         * @param dispatcher The dispatcher to be used when loading a class.
         */
        Default(Configurable dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
            return dispatcher.load(classLoader, types);
        }

        @Override
        public Configurable withProtectionDomain(ProtectionDomain protectionDomain) {
            return dispatcher.withProtectionDomain(protectionDomain);
        }

        @Override
        public Configurable withPackageDefinitionStrategy(PackageDefinitionStrategy packageDefinitionStrategy) {
            return dispatcher.withPackageDefinitionStrategy(packageDefinitionStrategy);
        }

        @Override
        public Configurable withAccessControlContext(AccessControlContext accessControlContext) {
            return dispatcher.withAccessControlContext(accessControlContext);
        }

        @Override
        public Configurable allowExistingTypes() {
            return dispatcher.allowExistingTypes();
        }

        @Override
        public String toString() {
            return "ClassLoadingStrategy.Default." + name();
        }

        /**
         * A class loading strategy which applies a class loader injection while applying a given
         * {@link java.security.ProtectionDomain} on class injection.
         */
        protected static class InjectionDispatcher implements ClassLoadingStrategy.Configurable {

            /**
             * The protection domain to apply.
             */
            private final ProtectionDomain protectionDomain;

            /**
             * The access control context to use for loading classes.
             */
            private final AccessControlContext accessControlContext;

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
                this(DEFAULT_PROTECTION_DOMAIN, AccessController.getContext(), PackageDefinitionStrategy.NoOp.INSTANCE, DEFAULT_FORBID_EXISTING);
            }

            /**
             * Creates a new injection dispatcher.
             *
             * @param protectionDomain          The protection domain to apply.
             * @param accessControlContext      The access control context to use for loading classes.
             * @param packageDefinitionStrategy The package definer to be used for querying information on package information.
             * @param forbidExisting            Determines if an exception should be thrown when attempting to load a type that already exists.
             */
            private InjectionDispatcher(ProtectionDomain protectionDomain,
                                        AccessControlContext accessControlContext,
                                        PackageDefinitionStrategy packageDefinitionStrategy,
                                        boolean forbidExisting) {
                this.protectionDomain = protectionDomain;
                this.accessControlContext = accessControlContext;
                this.packageDefinitionStrategy = packageDefinitionStrategy;
                this.forbidExisting = forbidExisting;
            }

            @Override
            public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
                return new ClassInjector.UsingReflection(classLoader,
                        protectionDomain,
                        accessControlContext,
                        packageDefinitionStrategy,
                        forbidExisting).inject(types);
            }

            @Override
            public Configurable withProtectionDomain(ProtectionDomain protectionDomain) {
                return new InjectionDispatcher(protectionDomain, accessControlContext, packageDefinitionStrategy, forbidExisting);
            }

            @Override
            public Configurable withPackageDefinitionStrategy(PackageDefinitionStrategy packageDefinitionStrategy) {
                return new InjectionDispatcher(protectionDomain, accessControlContext, packageDefinitionStrategy, forbidExisting);
            }

            @Override
            public Configurable withAccessControlContext(AccessControlContext accessControlContext) {
                return new InjectionDispatcher(protectionDomain, accessControlContext, packageDefinitionStrategy, forbidExisting);
            }

            @Override
            public Configurable allowExistingTypes() {
                return new InjectionDispatcher(protectionDomain, accessControlContext, packageDefinitionStrategy, false);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                InjectionDispatcher that = (InjectionDispatcher) other;
                return !(protectionDomain != null ? !protectionDomain.equals(that.protectionDomain) : that.protectionDomain != null)
                        && accessControlContext.equals(that.accessControlContext)
                        && forbidExisting == that.forbidExisting
                        && packageDefinitionStrategy.equals(that.packageDefinitionStrategy);
            }

            @Override
            public int hashCode() {
                int result = protectionDomain != null ? protectionDomain.hashCode() : 0;
                result = 31 * result + packageDefinitionStrategy.hashCode();
                result = 31 * result + accessControlContext.hashCode();
                result = 31 * result + (forbidExisting ? 1 : 0);
                return result;
            }

            @Override
            public String toString() {
                return "ClassLoadingStrategy.Default.InjectionDispatcher{" +
                        "protectionDomain=" + protectionDomain +
                        ", accessControlContext=" + accessControlContext +
                        ", packageDefinitionStrategy=" + packageDefinitionStrategy +
                        ", forbidExisting=" + forbidExisting +
                        '}';
            }
        }

        /**
         * A class loading strategy which creates a wrapping class loader while applying a given
         * {@link java.security.ProtectionDomain} on class loading.
         */
        protected static class WrappingDispatcher implements ClassLoadingStrategy.Configurable {

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
             * The access control context to use for loading classes.
             */
            private final AccessControlContext accessControlContext;

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
                this(DEFAULT_PROTECTION_DOMAIN,
                        AccessController.getContext(),
                        PackageDefinitionStrategy.Trivial.INSTANCE,
                        persistenceHandler,
                        childFirst,
                        DEFAULT_FORBID_EXISTING);
            }

            /**
             * Creates a new protection domain specific class loading wrapper.
             *
             * @param protectionDomain          The protection domain to apply.
             * @param accessControlContext      The access control context to use for loading classes.
             * @param packageDefinitionStrategy The package definer to be used for querying information on package information.
             * @param persistenceHandler        The persistence handler to apply.
             * @param childFirst                {@code true} if the created class loader should apply child-first semantics.
             * @param forbidExisting            Determines if an exception should be thrown when attempting to load a type that already exists.
             */
            private WrappingDispatcher(ProtectionDomain protectionDomain,
                                       AccessControlContext accessControlContext,
                                       PackageDefinitionStrategy packageDefinitionStrategy,
                                       ByteArrayClassLoader.PersistenceHandler persistenceHandler,
                                       boolean childFirst,
                                       boolean forbidExisting) {
                this.protectionDomain = protectionDomain;
                this.accessControlContext = accessControlContext;
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
                        accessControlContext,
                        persistenceHandler,
                        packageDefinitionStrategy,
                        childFirst,
                        forbidExisting);
            }

            @Override
            public Configurable withProtectionDomain(ProtectionDomain protectionDomain) {
                return new WrappingDispatcher(protectionDomain, accessControlContext, packageDefinitionStrategy, persistenceHandler, childFirst, forbidExisting);
            }

            @Override
            public Configurable withPackageDefinitionStrategy(PackageDefinitionStrategy packageDefinitionStrategy) {
                return new WrappingDispatcher(protectionDomain, accessControlContext, packageDefinitionStrategy, persistenceHandler, childFirst, forbidExisting);
            }

            @Override
            public Configurable withAccessControlContext(AccessControlContext accessControlContext) {
                return new WrappingDispatcher(protectionDomain, accessControlContext, packageDefinitionStrategy, persistenceHandler, childFirst, forbidExisting);
            }

            @Override
            public Configurable allowExistingTypes() {
                return new InjectionDispatcher(protectionDomain, accessControlContext, packageDefinitionStrategy, false);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                WrappingDispatcher that = (WrappingDispatcher) other;
                return childFirst == that.childFirst
                        && forbidExisting == that.forbidExisting
                        && !(protectionDomain != null ? !protectionDomain.equals(that.protectionDomain) : that.protectionDomain != null)
                        && persistenceHandler == that.persistenceHandler
                        && accessControlContext.equals(that.accessControlContext)
                        && packageDefinitionStrategy.equals(that.packageDefinitionStrategy);
            }

            @Override
            public int hashCode() {
                int result = protectionDomain != null ? protectionDomain.hashCode() : 0;
                result = 31 * result + accessControlContext.hashCode();
                result = 31 * result + persistenceHandler.hashCode();
                result = 31 * result + packageDefinitionStrategy.hashCode();
                result = 31 * result + (childFirst ? 1 : 0);
                result = 31 * result + (forbidExisting ? 1 : 0);
                return result;
            }

            @Override
            public String toString() {
                return "ClassLoadingStrategy.Default.WrappingDispatcher{" +
                        "packageDefinitionStrategy=" + packageDefinitionStrategy +
                        ", protectionDomain=" + protectionDomain +
                        ", accessControlContext=" + accessControlContext +
                        ", childFirst=" + childFirst +
                        ", persistenceHandler=" + persistenceHandler +
                        ", forbidExisting=" + forbidExisting +
                        '}';
            }
        }
    }

    /**
     * A {@link ClassLoadingStrategy} that allows configuring the strategy's behavior.
     */
    interface Configurable extends ClassLoadingStrategy {

        /**
         * Overrides the implicitly set default {@link java.security.ProtectionDomain} with an explicit one.
         *
         * @param protectionDomain The protection domain to apply.
         * @return This class loading strategy with an explicitly set {@link java.security.ProtectionDomain}.
         */
        Configurable withProtectionDomain(ProtectionDomain protectionDomain);

        /**
         * Defines the supplied package definition strategy to be used for defining packages.
         *
         * @param packageDefinitionStrategy The package definer to be used.
         * @return A version of this class loading strategy that applies the supplied package definition strategy.
         */
        Configurable withPackageDefinitionStrategy(PackageDefinitionStrategy packageDefinitionStrategy);

        /**
         * Defines the supplied access control context to be used for loading classes.
         *
         * @param accessControlContext The access control context to use for loading classes.
         * @return A version of this class loading strategy that applies the supplied access control context.
         */
        Configurable withAccessControlContext(AccessControlContext accessControlContext);

        /**
         * Determines if this class loading strategy should not throw an exception when attempting to load a class that
         * was already loaded. In this case, the already loaded class is used instead of the generated class.
         *
         * @return A version of this class loading strategy that does not throw an exception when a class is already loaded.
         */
        Configurable allowExistingTypes();
    }

    /**
     * A class loading strategy which allows class injection into the bootstrap class loader if
     * appropriate.
     */
    class ForBootstrapInjection implements ClassLoadingStrategy {

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

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            ForBootstrapInjection that = (ForBootstrapInjection) other;
            return folder.equals(that.folder)
                    && instrumentation.equals(that.instrumentation);
        }

        @Override
        public int hashCode() {
            int result = instrumentation.hashCode();
            result = 31 * result + folder.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ClassLoadingStrategy.ForBootstrapInjection{" +
                    "instrumentation=" + instrumentation +
                    ", folder=" + folder +
                    '}';
        }
    }
}
