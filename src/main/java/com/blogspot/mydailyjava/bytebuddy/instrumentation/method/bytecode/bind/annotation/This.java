package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface This {

    static enum Binder implements AnnotationDrivenBinder.ArgumentBinder<This> {
        INSTANCE;

        @Override
        public Class<This> getHandledType() {
            return This.class;
        }

        @Override
        public IdentifiedBinding<?> bind(This annotation,
                                         int targetParameterIndex,
                                         MethodDescription source,
                                         MethodDescription target,
                                         TypeDescription instrumentedType,
                                         Assigner assigner) {
            TypeDescription targetType = target.getParameterTypes().get(targetParameterIndex);
            if (targetType.isPrimitive()) {
                throw new IllegalStateException(String.format("The %d. argument of %s is a primitive type " +
                        "and can never be bound to an instance", targetParameterIndex, target));
            } else if(targetType.isArray()) {
                throw new IllegalStateException(String.format("The %d. argument of %s is an array type " +
                        "and can never be bound to an instance", targetParameterIndex, target));
            } else if (source.isStatic()) {
                return IdentifiedBinding.makeIllegal();
            }
            boolean runtimeType = RuntimeType.Verifier.check(target, targetParameterIndex);
            Assignment assignment = assigner.assign(instrumentedType, targetType, runtimeType);
            return IdentifiedBinding.makeAnonymous(new Assignment.Compound(MethodArgument.OBJECT_REFERENCE.loadFromIndex(0), assignment));
        }
    }
}
