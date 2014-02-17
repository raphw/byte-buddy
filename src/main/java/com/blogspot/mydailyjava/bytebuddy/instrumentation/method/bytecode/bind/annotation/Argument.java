package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.*;
import java.util.Iterator;
import java.util.LinkedHashSet;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Argument {

    static enum Binder implements AnnotationDrivenBinder.ArgumentBinder<Argument> {
        INSTANCE;

        @Override
        public Class<Argument> getHandledType() {
            return Argument.class;
        }

        @Override
        public IdentifiedBinding<?> bind(Argument sourceParameterIndex,
                                         int targetParameterIndex,
                                         MethodDescription source,
                                         MethodDescription target,
                                         TypeDescription instrumentedType,
                                         Assigner assigner) {
            if (sourceParameterIndex.value() < 0) {
                throw new IllegalArgumentException(String.format("Argument annotation on %d's argument virtual " +
                        "%s holds negative index", targetParameterIndex, target));
            } else if (source.getParameterTypes().size() <= sourceParameterIndex.value()) {
                return IdentifiedBinding.makeIllegal();
            }
            return makeBinding(source.getParameterTypes().get(sourceParameterIndex.value()),
                    target.getParameterTypes().get(targetParameterIndex),
                    sourceParameterIndex.value(),
                    assigner,
                    RuntimeType.Verifier.check(target, targetParameterIndex),
                    source.isStatic() ? 0 : 1);
        }

        private static IdentifiedBinding<?> makeBinding(TypeDescription sourceType,
                                                        TypeDescription targetType,
                                                        int sourceParameterIndex,
                                                        Assigner assigner,
                                                        boolean considerRuntimeType,
                                                        int sourceParameterOffset) {
            return IdentifiedBinding.makeIdentified(
                    new StackManipulation.Compound(
                            MethodArgument.forType(sourceType).loadFromIndex(sourceParameterIndex + sourceParameterOffset),
                            assigner.assign(sourceType, targetType, considerRuntimeType)),
                    new MostSpecificTypeResolver.ParameterIndexToken(sourceParameterIndex));
        }
    }

    static enum NextUnboundAsDefaultProvider implements AnnotationDrivenBinder.DefaultProvider {
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
        public Iterator<Argument> makeIterator(TypeDescription typeDescription, MethodDescription source, MethodDescription target) {
            return new NextUnboundArgumentIterator(makeFreeIndexList(source, target));
        }

        private static Iterator<Integer> makeFreeIndexList(MethodDescription source, MethodDescription target) {
            LinkedHashSet<Integer> results = new LinkedHashSet<Integer>(source.getParameterTypes().size());
            for (int sourceIndex = 0; sourceIndex < source.getParameterTypes().size(); sourceIndex++) {
                results.add(sourceIndex);
            }
            for (Annotation[] parameterAnnotation : target.getParameterAnnotations()) {
                for (Annotation aParameterAnnotation : parameterAnnotation) {
                    if (aParameterAnnotation.annotationType() == Argument.class) {
                        results.remove(((Argument) aParameterAnnotation).value());
                        break;
                    }
                }
            }
            return results.iterator();
        }
    }

    int value();
}
