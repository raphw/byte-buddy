package com.blogspot.mydailyjava.bytebuddy.dynamic;

import com.blogspot.mydailyjava.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import com.blogspot.mydailyjava.bytebuddy.dynamic.loading.ClassLoaderByteArrayInjector;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A strategy for loading a collection of types.
 */
public interface ClassLoadingStrategy {

    /**
     * Default class loading strategies.
     * <ol>
     * <li>The {@link com.blogspot.mydailyjava.bytebuddy.dynamic.ClassLoadingStrategy.Default#WRAPPER} strategy
     * will create a new {@link com.blogspot.mydailyjava.bytebuddy.dynamic.loading.ByteArrayClassLoader} which
     * has the given class loader as its parent. The byte array class loader is aware of a given number of types
     * and can natively load the given classes. This allows to load classes with cyclic dependencies since the byte
     * array class loader is queried on each encountered unknown class. Due to the class loader encapsulation of the
     * dynamic classes loaded by the byte array class loader, this strategy will lead to the unloading of these
     * classes once this class loader, its classes or any instances of these classes are not longer reachable.</li>
     * <li>The {@link com.blogspot.mydailyjava.bytebuddy.dynamic.ClassLoadingStrategy.Default#INJECTION} strategy
     * will not create a new class loader but inject all types into the given class loader by using reflection.
     * This prevents the loading of classes with cyclic dependencies but avoids the creation of an additional
     * class loader.</li>
     * </ol>
     */
    static enum Default implements ClassLoadingStrategy {

        WRAPPER,
        INJECTION;

        @Override
        public Collection<Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
            List<Class<?>> loadedTypes = new ArrayList<Class<?>>(types.size());
            switch (this) {
                case WRAPPER:
                    classLoader = new ByteArrayClassLoader(types, classLoader);
                    for (TypeDescription typeDescription : types.keySet()) {
                        try {
                            loadedTypes.add(classLoader.loadClass(typeDescription.getName()));
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("Cannot load class " + typeDescription, e);
                        }
                    }
                    break;
                case INJECTION:
                    ClassLoaderByteArrayInjector classLoaderByteArrayInjector = new ClassLoaderByteArrayInjector(classLoader);
                    for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
                        loadedTypes.add(classLoaderByteArrayInjector.inject(entry.getKey().getName(), entry.getValue()));
                    }
                    break;
                default:
                    throw new AssertionError();
            }
            return loadedTypes;
        }
    }

    /**
     * Loads a given collection of dynamically created types.
     *
     * @param classLoader The class loader to used for loading the classes.
     * @param types       Byte array representations of the types to be loaded mapped by their descriptions,
     *                    where an iteration order defines an order in which they are supposed to be loaded,
     *                    if relevant.
     * @return A collection of the loaded classes.
     */
    Collection<Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types);
}
