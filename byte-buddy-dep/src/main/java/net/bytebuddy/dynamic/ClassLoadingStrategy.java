package net.bytebuddy.dynamic;

import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoaderByteArrayInjector;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.util.LinkedHashMap;
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
    static enum Default implements ClassLoadingStrategy {

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
                Map<TypeDescription, Class<?>> loadedTypes = new LinkedHashMap<TypeDescription, Class<?>>(types.size());
                classLoader = ByteArrayClassLoader.of(classLoader, types, ByteArrayClassLoader.PersistenceHandler.LATENT);
                for (TypeDescription typeDescription : types.keySet()) {
                    try {
                        loadedTypes.put(typeDescription, classLoader.loadClass(typeDescription.getName()));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Cannot load class " + typeDescription, e);
                    }
                }
                return loadedTypes;
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
                Map<TypeDescription, Class<?>> loadedTypes = new LinkedHashMap<TypeDescription, Class<?>>(types.size());
                classLoader = ByteArrayClassLoader.of(classLoader, types, ByteArrayClassLoader.PersistenceHandler.MANIFEST);
                for (TypeDescription typeDescription : types.keySet()) {
                    try {
                        loadedTypes.put(typeDescription, classLoader.loadClass(typeDescription.getName()));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Cannot load class " + typeDescription, e);
                    }
                }
                return loadedTypes;
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
                Map<TypeDescription, Class<?>> loadedTypes = new LinkedHashMap<TypeDescription, Class<?>>(types.size());
                ClassLoaderByteArrayInjector classLoaderByteArrayInjector = new ClassLoaderByteArrayInjector(classLoader);
                for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
                    loadedTypes.put(entry.getKey(), classLoaderByteArrayInjector.inject(entry.getKey().getName(), entry.getValue()));
                }
                return loadedTypes;
            }
        }
    }
}
