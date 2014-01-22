package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.MostSpecificTypeResolver;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.LinkedHashSet;

public @interface Argument {

    static enum Binder implements AnnotationDrivenBinder.ArgumentBinder<Argument> {
        INSTANCE;

        @Override
        public Class<Argument> getHandledType() {
            return Argument.class;
        }

        @Override
        public IdentifiedBinding<?> bind(Argument sourceArgument,
                                         int targetParameterIndex,
                                         MethodDescription source,
                                         MethodDescription target,
                                         TypeDescription typeDescription,
                                         Assigner assigner) {
            if (source.getParameterTypes().length < sourceArgument.value()) {
                return IdentifiedBinding.makeIllegal();
            }
            Class<?> sourceType = source.getParameterTypes()[sourceArgument.value()];
            Class<?> targetType = target.getParameterTypes()[targetParameterIndex];
            return IdentifiedBinding.makeIdentified(
                    new Assignment.Compound(
                            MethodArgument.forType(sourceType).loadFromIndex(sourceArgument.value() + 1),
                            assigner.assign(sourceType, targetType, isRuntimeType(target, targetParameterIndex))),
                    new MostSpecificTypeResolver.ParameterIndexToken(targetParameterIndex));
        }

        private static boolean isRuntimeType(MethodDescription methodDescription, int parameterIndex) {
            for (Annotation annotation : methodDescription.getParameterAnnotations()[parameterIndex]) {
                if (annotation.annotationType() == RuntimeType.class) {
                    return true;
                }
            }
            return false;
        }
    }

    static enum NextUnboundAsDefaultHandler implements AnnotationDrivenBinder.AnnotationDefaultHandler {
        INSTANCE;

        private static class DefaultArgument implements Argument {

            private final int parameterIndex;

            private DefaultArgument(int parameterIndex) {
                this.parameterIndex = parameterIndex;
            }

            @Override
            public int value() {
                return parameterIndex;
            }

            @Override
            public Class<Argument> annotationType() {
                return Argument.class;
            }
        }

        private static class NextUnboundArgumentIterator implements Iterator<Argument> {

            private final Iterator<Integer> iterator;

            private NextUnboundArgumentIterator(Iterator<Integer> iterator) {
                this.iterator = iterator;
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Argument next() {
                return new DefaultArgument(iterator.next());
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        }

        @Override
        public Iterator<Argument> makeIterator(MethodDescription source, MethodDescription target) {
            return new NextUnboundArgumentIterator(makeFreeIndexList(source, target));
        }

        private static Iterator<Integer> makeFreeIndexList(MethodDescription source, MethodDescription target) {
            LinkedHashSet<Integer> results = new LinkedHashSet<Integer>(source.getParameterTypes().length);
            for (int sourceIndex = 0; sourceIndex < source.getParameterTypes().length; sourceIndex++) {
                results.add(sourceIndex);
            }
            for (Annotation[] parameterAnnotation : target.getParameterAnnotations()) {
                for (Annotation aParameterAnnotation : parameterAnnotation) {
                    if (aParameterAnnotation.annotationType() == Argument.class) {
                        results.remove(((Argument) aParameterAnnotation).value());
                    }
                }
            }
            return results.iterator();
        }
    }

    int value();
}
