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

    /**
     * A convenience variable representing the first index of an array, to make the code more readable.
     */
    private static final int FROM_BEGINNING = 0;

    /**
     * A storage for the reflection method representations that are obtained on loading this classes.
     */
    private static final ReflectionStore REFLECTION_STORE;

    /**
     * Obtains the reflective instances used by this injector or a no-op instance that throws the exception
     * that occurred when attempting to obtain the reflective member instances.
     */
    static {
        ReflectionStore reflectionStore;
        try {
            Method findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            findLoadedClassMethod.setAccessible(true);
            Method loadByteArrayMethod = ClassLoader.class.getDeclaredMethod("defineClass",
                    String.class,
                    byte[].class,
                    int.class,
                    int.class);
            loadByteArrayMethod.setAccessible(true);
            reflectionStore = new ReflectionStore.Resolved(findLoadedClassMethod, loadByteArrayMethod);
        } catch (Exception e) {
            reflectionStore = new ReflectionStore.Faulty(e);
        }
        REFLECTION_STORE = reflectionStore;
    }

    /**
     * The class loader into which the classes are to be injected.
     */
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
        try {
            synchronized (classLoader) {
                Class<?> type = (Class<?>) REFLECTION_STORE.getFindLoadedClassMethod().invoke(classLoader, name);
                if (type != null) {
                    return type;
                } else {
                    return (Class<?>) REFLECTION_STORE.getLoadByteArrayMethod().invoke(classLoader,
                            name,
                            binaryRepresentation,
                            FROM_BEGINNING,
                            binaryRepresentation.length);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not access injection method", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Exception on invoking loader method", e.getCause());
        }
    }

    @Override
    public String toString() {
        return "ClassLoaderByteArrayInjector{classLoader=" + classLoader + '}';
    }

    /**
     * A storage for method representations in order to access a class loader reflectively.
     */
    private static interface ReflectionStore {

        /**
         * Returns the method for finding a class on a class loader.
         *
         * @return The method for finding a class on a class loader.
         */
        Method getFindLoadedClassMethod();

        /**
         * Returns the method for loading a class into a class loader.
         *
         * @return The method for loading a class into a class loader.
         */
        Method getLoadByteArrayMethod();

        /**
         * Represents a successfully loaded method lookup.
         */
        static class Resolved implements ReflectionStore {

            /**
             * The method for finding a class on a class loader.
             */
            private final Method findLoadedClassMethod;
            /**
             * The method for loading a class into a class loader.
             */
            private final Method loadByteArrayMethod;

            /**
             * Creates a new resolved reflection store.
             *
             * @param findLoadedClassMethod The method for finding a class on a class loader.
             * @param loadByteArrayMethod   The method for loading a class into a class loader.
             */
            private Resolved(Method findLoadedClassMethod, Method loadByteArrayMethod) {
                this.findLoadedClassMethod = findLoadedClassMethod;
                this.loadByteArrayMethod = loadByteArrayMethod;
            }

            @Override
            public Method getFindLoadedClassMethod() {
                return findLoadedClassMethod;
            }

            @Override
            public Method getLoadByteArrayMethod() {
                return loadByteArrayMethod;
            }

            @Override
            public String toString() {
                return "ClassLoaderByteArrayInjector.ReflectionStore.Resolved{" +
                        "findLoadedClassMethod=" + findLoadedClassMethod +
                        ", loadByteArrayMethod=" + loadByteArrayMethod +
                        '}';
            }
        }

        /**
         * Represents an unsuccessfully loaded method lookup.
         */
        static class Faulty implements ReflectionStore {

            /**
             * An exception to throw when attempting to lookup a method using this reflection store.
             */
            private final RuntimeException exception;

            /**
             * Creates a new faulty reflection store.
             *
             * @param exception The exception that was thrown when attempting to lookup the method.
             */
            private Faulty(Exception exception) {
                this.exception = new RuntimeException("Could not execute reflective lookup", exception);
            }

            @Override
            public Method getFindLoadedClassMethod() {
                throw exception;
            }

            @Override
            public Method getLoadByteArrayMethod() {
                throw exception;
            }

            @Override
            public String toString() {
                return "ClassLoaderByteArrayInjector.ReflectionStore.Faulty{exception=" + exception + '}';
            }
        }
    }
}
