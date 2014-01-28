package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.collection.ArrayFactory;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface AllArguments {

    static enum Binder implements AnnotationDrivenBinder.ArgumentBinder<AllArguments> {
        INSTANCE;

        @Override
        public Class<AllArguments> getHandledType() {
            return AllArguments.class;
        }

        @Override
        public IdentifiedBinding<?> bind(AllArguments annotation,
                                         int targetParameterIndex,
                                         MethodDescription source,
                                         MethodDescription target,
                                         TypeDescription typeDescription,
                                         Assigner assigner) {
            Class<?> targetType = target.getParameterTypes()[targetParameterIndex];
            if (!targetType.isArray()) {
                throw new IllegalStateException(String.format("AllArgument annotation on %d's argument of " +
                        "%s does not point to array type", targetParameterIndex, target));
            }
            targetType = targetType.getComponentType();
            List<Assignment> assignments = new ArrayList<Assignment>(source.getParameterTypes().length);
            int sourceParameterOffset = source.isStatic() ? 0 : 1;
            boolean considerRuntimeType = RuntimeType.Verifier.check(target, targetParameterIndex);
            int index = 0;
            for (Class<?> sourceParameter : source.getParameterTypes()) {
                Assignment assignment = new Assignment.Compound(
                        MethodArgument.forType(sourceParameter).loadFromIndex(index + sourceParameterOffset),
                        assigner.assign(targetType, sourceParameter, considerRuntimeType));
                if (assignment.isValid()) {
                    assignments.add(assignment);
                } else {
                    return IdentifiedBinding.makeIllegal();
                }
            }
            return IdentifiedBinding.makeAnonymous(new ArrayFactory(targetType).withValues(assignments));
        }
    }
}
