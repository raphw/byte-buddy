package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.MostSpecificTypeResolver;

import java.lang.annotation.Annotation;

public @interface Argument {

    static class Binder implements AnnotationDrivenBinder.ArgumentBinder<Argument> {

        @Override
        public Class<Argument> getHandledType() {
            return Argument.class;
        }

        @Override
        public IdentifiedBinding<?> bind(Argument sourceArgument,
                                         int targetParameterIndex,
                                         MethodDescription source,
                                         MethodDescription target,
                                         Assigner assigner) {
            if (source.getParameterTypes().length < sourceArgument.value()) {
                return IdentifiedBinding.makeIllegal();
            }
            Class<?> sourceType = source.getParameterTypes()[sourceArgument.value()];
            Class<?> targetType = target.getParameterTypes()[targetParameterIndex];
            return IdentifiedBinding.makeIdentified(
                    new Assignment.Compound(
                            MethodArgument.forType(sourceType).loadingIndex(sourceArgument.value()),
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

    int value();
}
