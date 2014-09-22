package net.bytebuddy.dynamic.loading;

import net.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

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
     * A convenience reference to the default protection domain which is {@code null}.
     */
    private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

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
                    int.class,
                    ProtectionDomain.class);
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
     * The protection domain that is used when loading classes.
     */
    private final ProtectionDomain protectionDomain;

    /**
     * The access control context of this class loader's instantiation.
     */
    private final AccessControlContext accessControlContext;

    /**
     * Creates a new injector for the given {@link java.lang.ClassLoader} and a default
     * {@link java.security.ProtectionDomain}.
     *
     * @param classLoader The {@link java.lang.ClassLoader} into which new class definitions are to be injected.
     */
    public ClassLoaderByteArrayInjector(ClassLoader classLoader) {
        this(classLoader, DEFAULT_PROTECTION_DOMAIN);
    }

    /**
     * Creates a new injector for the given {@link java.lang.ClassLoader} and {@link java.security.ProtectionDomain}.
     *
     * @param classLoader      The {@link java.lang.ClassLoader} into which new class definitions are to be injected.
     * @param protectionDomain The protection domain to apply during class definition.
     */
    public ClassLoaderByteArrayInjector(ClassLoader classLoader, ProtectionDomain protectionDomain) {
        this.classLoader = classLoader;
        this.protectionDomain = protectionDomain;
        accessControlContext = AccessController.getContext();
    }

    /**
     * Injects a given type mapping into a class loader byte array injector.
     *
     * @param classLoaderByteArrayInjector The target of the injection.
     * @param types                        A mapping of types to their binary representation.
     * @return A map of loaded classes which were injected into the class loader byte array injector.
     */
    public static Map<TypeDescription, Class<?>> inject(ClassLoaderByteArrayInjector classLoaderByteArrayInjector,
                                                        Map<TypeDescription, byte[]> types) {
        Map<TypeDescription, Class<?>> loadedTypes = new LinkedHashMap<TypeDescription, Class<?>>(types.size());
        for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
            loadedTypes.put(entry.getKey(), classLoaderByteArrayInjector.inject(entry.getKey().getName(), entry.getValue()));
        }
        return loadedTypes;
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
                    try {
                        return AccessController.doPrivileged(new ClassLoadingAction(name, binaryRepresentation), accessControlContext);
                    } catch (PrivilegedActionException e) {
                        if (e.getCause() instanceof IllegalAccessException) {
                            throw (IllegalAccessException) e.getCause();
                        } else if (e.getCause() instanceof InvocationTargetException) {
                            throw (InvocationTargetException) e.getCause();
                        } else {
                            throw (RuntimeException) e.getCause();
                        }
                    }
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

    /**
     * A privileged action for loading a class reflectively.
     */
    private class ClassLoadingAction implements PrivilegedExceptionAction<Class<?>> {

        /**
         * A convenience variable representing the first index of an array, to make the code more readable.
         */
        private static final int FROM_BEGINNING = 0;

        /**
         * The name of the class that is being loaded.
         */
        private final String name;

        /**
         * The binary representation of the class that is being loaded.
         */
        private final byte[] binaryRepresentation;

        /**
         * Creates a new class loading action.
         *
         * @param name                 The name of the class that is being loaded.
         * @param binaryRepresentation The binary representation of the class that is being loaded.
         */
        private ClassLoadingAction(String name, byte[] binaryRepresentation) {
            this.name = name;
            this.binaryRepresentation = binaryRepresentation;
        }

        @Override
        public Class<?> run() throws IllegalAccessException, InvocationTargetException {
            return (Class<?>) REFLECTION_STORE.getLoadByteArrayMethod().invoke(classLoader,
                    name,
                    binaryRepresentation,
                    FROM_BEGINNING,
                    binaryRepresentation.length,
                    protectionDomain);
        }

        @Override
        public String toString() {
            return "ClassLoaderByteArrayInjector.ClassLoadingAction{" +
                    "injector=" + ClassLoaderByteArrayInjector.this +
                    ", name='" + name + '\'' +
                    ", binaryRepresentation=" + Arrays.toString(binaryRepresentation) +
                    '}';
        }
    }
}
