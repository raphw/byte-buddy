package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.collection.ArrayFactory;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

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
                                         TypeDescription instrumentedType,
                                         Assigner assigner) {
            TypeDescription targetType = target.getParameterTypes().get(targetParameterIndex);
            ArrayFactory arrayFactory = ArrayFactory.of(targetType);
            List<Assignment> assignments = new ArrayList<Assignment>(source.getParameterTypes().size());
            int sourceParameterOffset = source.isStatic() ? 0 : 1;
            boolean considerRuntimeType = RuntimeType.Verifier.check(target, targetParameterIndex);
            int index = 0;
            for (TypeDescription sourceParameter : source.getParameterTypes()) {
                Assignment assignment = new Assignment.Compound(
                        MethodArgument.forType(sourceParameter).loadFromIndex(index++ + sourceParameterOffset),
                        assigner.assign(sourceParameter, arrayFactory.getComponentType(), considerRuntimeType));
                if (assignment.isValid()) {
                    assignments.add(assignment);
                } else {
                    return IdentifiedBinding.makeIllegal();
                }
            }
            return IdentifiedBinding.makeAnonymous(arrayFactory.withValues(assignments));
        }
    }
}
