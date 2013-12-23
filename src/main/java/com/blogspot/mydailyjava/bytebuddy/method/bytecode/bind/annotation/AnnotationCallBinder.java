package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.context.MethodContext;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.BoundCall;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.CallBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotationCallBinder implements CallBinder {

    public static interface Handler<T extends Annotation> {

        Class<T> getHandledType();

        Assignment assign(Class<?> assignmentTarget, T argument, ClassContext classContext, MethodContext methodContext);
    }

    private static class DefaultArgument implements Argument {

        private final int index;

        private DefaultArgument(int index) {
            this.index = index;
        }

        @Override
        public int value() {
            return index;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Argument.class;
        }
    }

    private static class HandlerDelegate {

        private final Map<? extends Class<? extends Annotation>, Handler<Annotation>> handlerMap;

        private HandlerDelegate(Collection<? extends Handler<?>> handlers) {
            this.handlerMap = toHandlerMap(handlers);
        }

        @SuppressWarnings("unchecked")
        private Map<? extends Class<? extends Annotation>, Handler<Annotation>> toHandlerMap(Collection<? extends Handler<?>> handlers) {
            Map<Class<Annotation>, Handler<Annotation>> handlerMap = new HashMap<Class<Annotation>, Handler<Annotation>>(handlers.size());
            for (Handler<? extends Annotation> handler : handlers) {
                handlerMap.put((Class<Annotation>) handler.getHandledType(), (Handler<Annotation>) handler);
            }
            return handlerMap;
        }

        public Assignment apply(Map<Class<? extends Annotation>, ? extends Annotation> annotations,
                                Class<?> type, ClassContext classContext, MethodContext methodContext) {
            Annotation annotation = pick(findRelevant(annotations.keySet()), annotations);
            return applyHandler(annotation, handlerMap.get(annotation.annotationType()), type, classContext, methodContext);
        }

        private <T extends Annotation> Assignment applyHandler(T annotation, Handler<T> handler,
                                                               Class<?> type,
                                                               ClassContext classContext, MethodContext methodContext) {
            return handler.assign(type, annotation, classContext, methodContext);
        }

        private Annotation pick(Set<Class<? extends Annotation>> relevantAnnotations,
                                Map<Class<? extends Annotation>, ? extends Annotation> annotationMap) {
            if (relevantAnnotations.size() == 1) {
                return annotationMap.get(relevantAnnotations.iterator().next());
            } else if (relevantAnnotations.size() == 0) {
                return new DefaultArgument(-1);
            } else {
                throw new IllegalStateException("");
            }
        }

        private Set<Class<? extends Annotation>> findRelevant(Collection<Class<? extends Annotation>> annotations) {
            Set<Class<? extends Annotation>> relevantHandlers = new HashSet<Class<? extends Annotation>>(handlerMap.keySet());
            relevantHandlers.retainAll(annotations);
            return relevantHandlers;
        }
    }

    private final ClassContext classContext;
    private final MethodContext methodContext;

    private final HandlerDelegate handlerDelegate;

    public AnnotationCallBinder(ClassContext classContext, MethodContext methodContext, Collection<? extends Handler<?>> handlers) {
        this.classContext = classContext;
        this.methodContext = methodContext;
        this.handlerDelegate = new HandlerDelegate(handlers);
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
        return handlerDelegate.apply(annotations, type, classContext, methodContext);
    }

}
