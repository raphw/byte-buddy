package net.bytebuddy.agent.builder;

import java.lang.instrument.ClassFileTransformer;
import java.lang.invoke.LambdaConversionException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * This class serves as a dispatcher for creating lambda expression objects when Byte Buddy is configured to instrument the
 * {@code java.lang.invoke.LambdaMetafactory}. For this purpose, this class is injected into the class path to serve as a VM-global
 * singleton and for becoming reachable from the JVM's meta factory. This class keeps a reference to all registered transformers which need
 * to be explicitly deregistered in order to avoid a memory leak.
 */
public class LambdaFactory {

    /**
     * A mapping of all registered class file transformers and their lambda factories, linked in their application order.
     */
    public static final Map<ClassFileTransformer, LambdaFactory> CLASS_FILE_TRANSFORMERS = new LinkedHashMap<ClassFileTransformer, LambdaFactory>();

    /**
     * The target instance that is a factory for creating lambdas.
     */
    private final Object target;

    /**
     * The dispatcher method to invoke for creating a new lambda instance.
     */
    private final Method dispatcher;

    /**
     * Creates a new lambda factory.
     *
     * @param target     The target instance that is a factory for creating lambdas.
     * @param dispatcher The dispatcher method to invoke for creating a new lambda instance.
     */
    private LambdaFactory(Object target, Method dispatcher) {
        this.target = target;
        this.dispatcher = dispatcher;
    }

    @SuppressWarnings("all")
    public static boolean register(ClassFileTransformer classFileTransformer, Object lambdaCreator, Callable<Class<?>> injector) {
        try {
            @SuppressWarnings("unchecked")
            Map<ClassFileTransformer, Object> classFileTransformers = (Map<ClassFileTransformer, Object>) injector.call()
                    .getDeclaredField("CLASS_FILE_TRANSFORMERS")
                    .get(null);
            synchronized (classFileTransformers) {
                try {
                    return classFileTransformers.isEmpty();
                } finally {
                    classFileTransformers.put(classFileTransformer, new LambdaFactory(lambdaCreator, lambdaCreator.getClass().getDeclaredMethod("make",
                            Object.class,
                            String.class,
                            Object.class,
                            Object.class,
                            Object.class,
                            Object.class,
                            boolean.class,
                            List.class,
                            List.class,
                            Collection.class)));
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not register class file transformer", exception);
        }
    }

    @SuppressWarnings("all")
    public static boolean release(ClassFileTransformer classFileTransformer) {
        try {
            @SuppressWarnings("unchecked")
            Map<ClassFileTransformer, ?> classFileTransformers = (Map<ClassFileTransformer, ?>) ClassLoader.getSystemClassLoader()
                    .loadClass(LambdaFactory.class.getName())
                    .getDeclaredField("CLASS_FILE_TRANSFORMERS")
                    .get(null);
            synchronized (classFileTransformers) {
                return classFileTransformers.remove(classFileTransformer) != null && classFileTransformers.isEmpty();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not release class file transformer", exception);
        }
    }

    private byte[] invoke(Object caller,
                          String invokedName,
                          Object invokedType,
                          Object samMethodType,
                          Object implMethod,
                          Object instantiatedMethodType,
                          boolean serializable,
                          List<Class<?>> markerInterfaces,
                          List<?> additionalBridges,
                          Set<ClassFileTransformer> classFileTransformers) {

        try {
            return (byte[]) dispatcher.invoke(target,
                    caller,
                    invokedName,
                    invokedType,
                    samMethodType,
                    implMethod,
                    instantiatedMethodType,
                    serializable,
                    markerInterfaces,
                    additionalBridges,
                    classFileTransformers);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static byte[] make(Object caller,
                              String invokedName,
                              Object invokedType,
                              Object samMethodType,
                              Object implMethod,
                              Object instantiatedMethodType,
                              boolean serializable,
                              List<Class<?>> markerInterfaces,
                              List<?> additionalBridges) throws LambdaConversionException {
        return CLASS_FILE_TRANSFORMERS.values().iterator().next().invoke(caller,
                invokedName,
                invokedType,
                samMethodType,
                implMethod,
                instantiatedMethodType,
                serializable,
                markerInterfaces,
                additionalBridges,
                CLASS_FILE_TRANSFORMERS.keySet());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        LambdaFactory that = (LambdaFactory) other;
        return target.equals(that.target) && dispatcher.equals(that.dispatcher);
    }

    @Override
    public int hashCode() {
        int result = target.hashCode();
        result = 31 * result + dispatcher.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "LambdaFactory{" +
                "target=" + target +
                ", dispatcher=" + dispatcher +
                '}';
    }
}
