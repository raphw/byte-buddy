package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.context.MethodContext;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.BoundCall;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.CallBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class AnnotationCallBinder implements CallBinder {

    private static class DefaultArgument implements Argument {

        private final int index;

        private DefaultArgument(int index) {
            this.index = index;
        }

        @Override
        public int value() {
            return 0;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Argument.class;
        }
    }

    public static interface Handler<T extends Annotation> {

        Class<T> getHandledType();

        Assignment assign(Class<?> assignmentTarget, T argument, ClassContext classContext, MethodContext methodContext);
    }

    private final ClassContext classContext;
    private final MethodContext methodContext;

    public AnnotationCallBinder(ClassContext classContext, MethodContext methodContext) {
        this.classContext = classContext;
        this.methodContext = methodContext;
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
        Map<Class<? extends Annotation>, Annotation> annotationMap =
                new HashMap<Class<? extends Annotation>, Annotation>(annotation.length);
        for (Annotation a : annotation) {
            annotationMap.put(a.annotationType(), a);
        }
        return annotationMap;
    }

    private Assignment tryBind(Class<?> type, Map<Class<? extends Annotation>, Annotation> annotations) {
        return IllegalAssignment.INSTANCE;
    }

}
