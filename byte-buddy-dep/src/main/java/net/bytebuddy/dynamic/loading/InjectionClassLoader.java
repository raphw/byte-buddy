package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;

import java.util.HashMap;
import java.util.Map;

/**
 * An injection class loader allows for the injection of a class after the class loader was created.
 */
public abstract class InjectionClassLoader extends ClassLoader {

    /**
     * Creates a new injection class loader.
     *
     * @param parent The class loader's parent.
     */
    protected InjectionClassLoader(ClassLoader parent) {
        super(parent);
    }

    /**
     * Defines a new type to be loaded by this class loader. If a type with the same name was already defined, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param name                 The name of the type.
     * @param binaryRepresentation The type's binary representation.
     * @return The defined class or a previously defined class.
     * @throws ClassNotFoundException If the class could not be loaded.
     */
    public abstract Class<?> defineClass(String name, byte[] binaryRepresentation) throws ClassNotFoundException;

    /**
     * A class loading strategy for adding a type to an injection class loader.
     */
    public enum Strategy implements ClassLoadingStrategy<InjectionClassLoader> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Map<TypeDescription, Class<?>> load(InjectionClassLoader classLoader, Map<TypeDescription, byte[]> types) {
            if (classLoader == null) {
                throw new IllegalArgumentException("Cannot add types to bootstrap class loader: " + types);
            }
            Map<TypeDescription, Class<?>> loadedTypes = new HashMap<TypeDescription, Class<?>>();
            try {
                for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
                    loadedTypes.put(entry.getKey(), classLoader.defineClass(entry.getKey().getName(), entry.getValue()));
                }
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Cannot load classes: " + types, exception);
            }
            return loadedTypes;
        }
    }
}
