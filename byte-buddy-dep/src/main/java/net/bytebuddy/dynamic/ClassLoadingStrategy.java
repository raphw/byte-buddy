package net.bytebuddy.dynamic;

import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoaderByteArrayInjector;
import net.bytebuddy.instrumentation.type.TypeDescription;

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
    static enum Default implements WithDefaultProtectionDomain {

        /**
         * This strategy creates a new {@link net.bytebuddy.dynamic.loading.ByteArrayClassLoader} with the given
         * class loader as its parent. The byte array class loader is aware of a any dynamically created type and can
         * natively load the given classes. This allows to load classes with cyclic load-time dependencies since the
         * byte array class loader is queried on each encountered unknown class. Due to the encapsulation of the
         * classes that were loaded by a byte array class loader, this strategy will lead to the unloading of these
         * classes once this class loader, its classes or any instances of these classes become unreachable.
         */
        WRAPPER {
            @Override
            public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
                return ByteArrayClassLoader.load(classLoader,
                        types,
                        DEFAULT_PROTECTION_DOMAIN,
                        ByteArrayClassLoader.PersistenceHandler.LATENT, PARENT_FIRST);
            }

            @Override
            public ClassLoadingStrategy withProtectionDomain(ProtectionDomain protectionDomain) {
                return new ProtectionDomainWrapper(protectionDomain,
                        ByteArrayClassLoader.PersistenceHandler.LATENT,
                        PARENT_FIRST);
            }
        },

        /**
         * The strategy is identical to {@link net.bytebuddy.dynamic.ClassLoadingStrategy.Default#WRAPPER} but exposes
         * the byte arrays that represent a class by {@link java.lang.ClassLoader#getResourceAsStream(String)}. For
         * this purpose, all class files are persisted as byte arrays withing the wrapping class loader.
         */
        WRAPPER_PERSISTENT {
            @Override
            public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
                return ByteArrayClassLoader.load(classLoader,
                        types,
                        DEFAULT_PROTECTION_DOMAIN,
                        ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                        PARENT_FIRST);
            }

            @Override
            public ClassLoadingStrategy withProtectionDomain(ProtectionDomain protectionDomain) {
                return new ProtectionDomainWrapper(protectionDomain,
                        ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                        PARENT_FIRST);
            }
        },

        /**
         * <p>
         * The child-first class loading strategy is a modified version of the
         * {@link net.bytebuddy.dynamic.ClassLoadingStrategy.Default#WRAPPER} where the dynamic types are given
         * priority over any types of a parent class loader with the same name.
         * </p>
         * <p>
         * <b>Important</b>: This does <i>not</i> replace a type of the same name, but it makes the type invisible by
         * the reach of this class loader.
         * </p>
         */
        CHILD_FIRST {
            @Override
            public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
                return ByteArrayClassLoader.load(classLoader,
                        types,
                        DEFAULT_PROTECTION_DOMAIN,
                        ByteArrayClassLoader.PersistenceHandler.LATENT,
                        PARENT_LAST);
            }

            @Override
            public ClassLoadingStrategy withProtectionDomain(ProtectionDomain protectionDomain) {
                return new ProtectionDomainWrapper(protectionDomain,
                        ByteArrayClassLoader.PersistenceHandler.LATENT,
                        PARENT_LAST);
            }
        },

        /**
         * The strategy is identical to {@link net.bytebuddy.dynamic.ClassLoadingStrategy.Default#CHILD_FIRST} but
         * exposes the byte arrays that represent a class by {@link java.lang.ClassLoader#getResourceAsStream(String)}.
         * For this purpose, all class files are persisted as byte arrays withing the wrapping class loader.
         */
        CHILD_FIRST_PERSISTENT {
            @Override
            public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
                return ByteArrayClassLoader.load(classLoader,
                        types,
                        DEFAULT_PROTECTION_DOMAIN,
                        ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                        PARENT_LAST);
            }

            @Override
            public ClassLoadingStrategy withProtectionDomain(ProtectionDomain protectionDomain) {
                return new ProtectionDomainWrapper(protectionDomain,
                        ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                        PARENT_LAST);
            }
        },

        /**
         * This strategy does not create a new class loader but injects all classes into the given {@link java.lang.ClassLoader}
         * by reflective access. This prevents the loading of classes with cyclic load-time dependencies but avoids the
         * creation of an additional class loader. The advantage of this strategy is that the loaded classes will have
         * package-private access to other classes within their package of the class loader into which they are
         * injected what is not permitted when the wrapper class loader is used. This strategy is implemented using a
         * {@link net.bytebuddy.dynamic.loading.ClassLoaderByteArrayInjector}. Note that this strategy usually yields
         * a better runtime performance.
         */
        INJECTION {
            @Override
            public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
                return ClassLoaderByteArrayInjector.inject(new ClassLoaderByteArrayInjector(classLoader), types);
            }

            @Override
            public ClassLoadingStrategy withProtectionDomain(ProtectionDomain protectionDomain) {
                return new ProtectionDomainInjection(protectionDomain);
            }
        };

        /**
         * An identifier for a class loading-order for making the code more readable.
         */
        private static final boolean PARENT_LAST = true, PARENT_FIRST = false;

        /**
         * A convenience reference that references the default protection domain which is {@code null}.
         */
        private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

        /**
         * A class loading strategy which applies a class loader injection while applying a given
         * {@link java.security.ProtectionDomain} on class injection.
         */
        protected static class ProtectionDomainInjection implements ClassLoadingStrategy {

            /**
             * The protection domain to apply.
             */
            private final ProtectionDomain protectionDomain;

            /**
             * Creates a new protection domain injection class loading strategy.
             *
             * @param protectionDomain The protection domain to apply.
             */
            protected ProtectionDomainInjection(ProtectionDomain protectionDomain) {
                this.protectionDomain = protectionDomain;
            }

            @Override
            public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
                return ClassLoaderByteArrayInjector.inject(new ClassLoaderByteArrayInjector(classLoader, protectionDomain), types);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && protectionDomain.equals(((ProtectionDomainInjection) other).protectionDomain);
            }

            @Override
            public int hashCode() {
                return protectionDomain.hashCode();
            }

            @Override
            public String toString() {
                return "ClassLoadingStrategy.Default.ProtectionDomainInjection{protectionDomain=" + protectionDomain + '}';
            }
        }

        /**
         * A class loading strategy which creates a wrapping class loader while applying a given
         * {@link java.security.ProtectionDomain} on class loading.
         */
        protected static class ProtectionDomainWrapper implements ClassLoadingStrategy {

            /**
             * The protection domain to apply.
             */
            private final ProtectionDomain protectionDomain;

            /**
             * The persistence handler to apply.
             */
            private final ByteArrayClassLoader.PersistenceHandler persistenceHandler;

            /**
             * {@code true} if the created class loader should apply child-first semantics.
             */
            private final boolean childFirst;

            /**
             * Creates a new protection domain specific class loading wrapper.
             *
             * @param protectionDomain   The protection domain to apply.
             * @param persistenceHandler The persistence handler to apply.
             * @param childFirst         {@code true} if the created class loader should apply child-first semantics.
             */
            public ProtectionDomainWrapper(ProtectionDomain protectionDomain,
                                           ByteArrayClassLoader.PersistenceHandler persistenceHandler,
                                           boolean childFirst) {
                this.protectionDomain = protectionDomain;
                this.persistenceHandler = persistenceHandler;
                this.childFirst = childFirst;
            }

            @Override
            public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
                return ByteArrayClassLoader.load(classLoader, types, protectionDomain, persistenceHandler, childFirst);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                ProtectionDomainWrapper that = (ProtectionDomainWrapper) other;
                return childFirst == that.childFirst
                        && persistenceHandler == that.persistenceHandler
                        && protectionDomain.equals(that.protectionDomain);
            }

            @Override
            public int hashCode() {
                int result = protectionDomain.hashCode();
                result = 31 * result + (childFirst ? 1 : 0);
                result = 31 * result + persistenceHandler.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "ClassLoadingStrategy.Default.ProtectionDomainWrapper{" +
                        "protectionDomain=" + protectionDomain +
                        ", childFirst=" + childFirst +
                        ", persistenceHandler=" + persistenceHandler +
                        '}';
            }
        }
    }

    /**
     * A {@link net.bytebuddy.dynamic.ClassLoadingStrategy} that applies a default {@link java.security.ProtectionDomain}.
     */
    static interface WithDefaultProtectionDomain extends ClassLoadingStrategy {

        /**
         * Overrides the implicitly set default {@link java.security.ProtectionDomain} with an explicit one.
         *
         * @param protectionDomain The protection domain to apply.
         * @return This class loading strategy with an explicitly set {@link java.security.ProtectionDomain}.
         */
        ClassLoadingStrategy withProtectionDomain(ProtectionDomain protectionDomain);
    }
}
