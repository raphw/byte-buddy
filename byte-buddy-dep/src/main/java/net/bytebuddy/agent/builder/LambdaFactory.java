package net.bytebuddy.agent.builder;

import java.lang.instrument.ClassFileTransformer;
import java.lang.invoke.LambdaConversionException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class LambdaFactory {

    public static final Map<ClassFileTransformer, Class<?>> CLASS_FILE_TRANSFORMERS = Collections.synchronizedMap(new LinkedHashMap<ClassFileTransformer, Class<?>>());

    public static byte[] make(Object caller,
                              String invokedName,
                              Object invokedType,
                              Object samMethodType,
                              Object implMethod) throws LambdaConversionException {
        try {
            return (byte[]) CLASS_FILE_TRANSFORMERS.values().iterator().next()
                    .getDeclaredMethod("make", Object.class, String.class, Object.class, Object.class, Object.class, Collection.class)
                    .invoke(null, caller, invokedName, invokedType, samMethodType, implMethod, CLASS_FILE_TRANSFORMERS.keySet());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
