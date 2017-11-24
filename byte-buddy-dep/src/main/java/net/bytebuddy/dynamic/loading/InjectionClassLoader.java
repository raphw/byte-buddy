package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
     * Defines a new type to be loaded by this class loader.
     *
     * @param name                 The name of the type.
     * @param binaryRepresentation The type's binary representation.
     * @return The defined class or a previously defined class.
     * @throws ClassNotFoundException If the class could not be loaded.
     */
    public abstract Class<?> defineClass(String name, byte[] binaryRepresentation) throws ClassNotFoundException;

    /**
     * Defines a group of types to be loaded by this class loader.
     *
     * @param typeDefinitions The types binary representations.
     * @return The mapping of defined classes or previously defined classes by their name.
     * @throws ClassNotFoundException If the class could not be loaded.
     */
    public abstract Map<String, Class<?>> defineClasses(Map<String, byte[]> typeDefinitions) throws ClassNotFoundException;

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
            Map<String, byte[]> typeDefinitions = new LinkedHashMap<String, byte[]>();
            Map<String, TypeDescription> typeDescriptions = new HashMap<String, TypeDescription>();
            for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
                typeDefinitions.put(entry.getKey().getName(), entry.getValue());
                typeDescriptions.put(entry.getKey().getName(), entry.getKey());
            }
            Map<TypeDescription, Class<?>> loadedTypes = new HashMap<TypeDescription, Class<?>>();
            try {
                for (Map.Entry<String, Class<?>> entry : classLoader.defineClasses(typeDefinitions).entrySet()) {
                    loadedTypes.put(typeDescriptions.get(entry.getKey()), entry.getValue());
                }
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Cannot load classes: " + types, exception);
            }
            return loadedTypes;
        }
    }
}
