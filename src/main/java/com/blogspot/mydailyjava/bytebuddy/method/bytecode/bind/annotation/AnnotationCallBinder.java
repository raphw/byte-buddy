package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.BoundCall;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.CallBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AnnotationCallBinder implements CallBinder {

    public static interface ArgumentHandler<T extends Annotation> {

        Class<T> getHandledType();

        Assignment assign(int parameterIndex, T annotation, Method sourceMethod, Method targetMethod);
    }


    public AnnotationCallBinder(Collection<? extends ArgumentHandler<?>> handlers) {
    }

    @Override
    public BoundCall bind(Method target) {
        Class<?>[] argumentType = target.getParameterTypes();
        Annotation[][] argumentAnnotations = target.getParameterAnnotations();
        for (int i = 0; i < argumentType.length; i++) {
            tryBind(argumentType[i], toMap(argumentAnnotations[i]));
        }
        return null;
    }

    private static Map<Class<? extends Annotation>, Annotation> toMap(Annotation[] annotation) {
        Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>(annotation.length);
        for (Annotation a : annotation) {
            annotationMap.put(a.annotationType(), a);
        }
        return annotationMap;
    }

    private Assignment tryBind(Class<?> type, Map<Class<? extends Annotation>, Annotation> annotations) {
        return null;
    }

}
