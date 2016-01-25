package net.bytebuddy.agent.builder;

import java.lang.instrument.ClassFileTransformer;
import java.lang.invoke.LambdaConversionException;
import java.lang.reflect.Method;
import java.util.*;

public class LambdaFactory {

    public static final Map<ClassFileTransformer, LambdaFactory> CLASS_FILE_TRANSFORMERS = new LinkedHashMap<ClassFileTransformer, LambdaFactory>();

    private final Object target;

    private final Method dispatcher;

    private LambdaFactory(Object target, Method dispatcher) {
        this.target = target;
        this.dispatcher = dispatcher;
    }

    public static boolean register(ClassFileTransformer classFileTransformer, Object lambdaCreator) {
        try {
            @SuppressWarnings("unchecked")
            Map<ClassFileTransformer, LambdaFactory> classFileTransformers = (Map<ClassFileTransformer, LambdaFactory>) ClassLoader.getSystemClassLoader()
                    .loadClass(LambdaFactory.class.getName())
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
                            Collection.class)));
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not register class file transformer", exception);
        }
    }

    public static boolean release(ClassFileTransformer classFileTransformer) {
        try {
            @SuppressWarnings("unchecked")
            Map<ClassFileTransformer, LambdaFactory> classFileTransformers = (Map<ClassFileTransformer, LambdaFactory>) ClassLoader.getSystemClassLoader()
                    .loadClass(LambdaFactory.class.getName())
                    .getDeclaredField("CLASS_FILE_TRANSFORMERS")
                    .get(null);
            synchronized (classFileTransformers) {
                classFileTransformers.remove(classFileTransformer);
                return classFileTransformers.isEmpty();
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
                          boolean enforceSerialization,
                          List<Class<?>> markerInterfaces,
                          Set<ClassFileTransformer> classFileTransformers) {

        try {
            return (byte[]) dispatcher.invoke(target,
                    caller,
                    invokedName,
                    invokedType,
                    samMethodType,
                    implMethod,
                    instantiatedMethodType,
                    enforceSerialization,
                    markerInterfaces,
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
                              boolean enforceSerialization,
                              List<Class<?>> markerInterfaces) throws LambdaConversionException {
            return CLASS_FILE_TRANSFORMERS.values().iterator().next().invoke(caller,
                    invokedName,
                    invokedType,
                    samMethodType,
                    implMethod,
                    instantiatedMethodType,
                    enforceSerialization,
                    markerInterfaces,
                    CLASS_FILE_TRANSFORMERS.keySet());
    }
}
