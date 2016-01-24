package net.bytebuddy.agent.builder;

import java.lang.instrument.ClassFileTransformer;
import java.lang.invoke.LambdaConversionException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class LambdaFactory {

    public static final Map<ClassFileTransformer, Method> CLASS_FILE_TRANSFORMERS = new LinkedHashMap<ClassFileTransformer, Method>();

    private LambdaFactory() {
        throw new UnsupportedOperationException("This instance is meant as a lambda factory dispatcher and should not be instantiated");
    }

    public static boolean register(ClassFileTransformer classFileTransformer, Class<?> factory) {
        try {
            @SuppressWarnings("unchecked")
            Map<ClassFileTransformer, Method> classFileTransformers = (Map<ClassFileTransformer, Method>) ClassLoader.getSystemClassLoader()
                    .loadClass(LambdaFactory.class.getName())
                    .getDeclaredField("CLASS_FILE_TRANSFORMERS")
                    .get(null);
            synchronized (classFileTransformers) {
                try {
                    return classFileTransformers.isEmpty();
                } finally {
                    classFileTransformers.put(classFileTransformer, factory.getDeclaredMethod("make",
                            Object.class,
                            String.class,
                            Object.class,
                            Object.class,
                            Object.class,
                            Collection.class));
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not register class file transformer", exception);
        }
    }

    public static boolean release(ClassFileTransformer classFileTransformer) {
        try {
            @SuppressWarnings("unchecked")
            Map<ClassFileTransformer, Method> classFileTransformers = (Map<ClassFileTransformer, Method>) ClassLoader.getSystemClassLoader()
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

    public static byte[] make(Object caller,
                              String invokedName,
                              Object invokedType,
                              Object samMethodType,
                              Object implMethod) throws LambdaConversionException {
        try {
            return (byte[]) CLASS_FILE_TRANSFORMERS.values().iterator().next()
                    .invoke(null, caller, invokedName, invokedType, samMethodType, implMethod, CLASS_FILE_TRANSFORMERS.keySet());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
