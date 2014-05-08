package net.bytebuddy.dynamic.loading;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An injector that loads classes by reflectively invoking non-public methods on a given {@link java.lang.ClassLoader}.
 * <p>&nbsp;</p>
 * Note that the injector is only able to load classes in a linear manner. Thus, classes that refer to other classes
 * which are not yet loaded cannot be injected but will result in a {@link java.lang.NoClassDefFoundError}. This becomes
 * a problem when classes refer to each other using cyclic references. This injector can further not be applied to the
 * bootstrap class loader which is usually represented by a {@code null} value and can therefore not be accessed by
 * reflection.
 */
public class ClassLoaderByteArrayInjector {

    private static Method findLoadedClassMethod;
    private static Method loadByteArrayMethod;

    private static Exception exception;

    static {
        try {
            findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            findLoadedClassMethod.setAccessible(true);
            loadByteArrayMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            loadByteArrayMethod.setAccessible(true);
        } catch (Exception e) {
            exception = e;
        }
    }

    private final ClassLoader classLoader;

    /**
     * Creates a new injector for the given {@link java.lang.ClassLoader}.
     *
     * @param classLoader The {@link java.lang.ClassLoader} into which new class definitions are to be injected.
     */
    public ClassLoaderByteArrayInjector(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Explicitly loads a {@link java.lang.Class} by reflective access into the represented class loader.
     *
     * @param name                 The fully qualified name of the {@link java.lang.Class} to be loaded.
     * @param binaryRepresentation The type's binary representation.
     * @return The loaded class that is a result of the class loading attempt.
     */
    public Class<?> inject(String name, byte[] binaryRepresentation) {
        if (findLoadedClassMethod == null || loadByteArrayMethod == null) {
            throw new IllegalStateException("Could not initialize class loader injector", exception);
        }
        try {
            synchronized (classLoader) {
                Class<?> type = (Class<?>) findLoadedClassMethod.invoke(classLoader, name);
                if (type != null) {
                    return type;
                } else {
                    return (Class<?>) loadByteArrayMethod.invoke(classLoader,
                            name,
                            binaryRepresentation,
                            0,
                            binaryRepresentation.length);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not access injection method", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Exception on invoking loader method", e.getCause());
        }
    }
}
