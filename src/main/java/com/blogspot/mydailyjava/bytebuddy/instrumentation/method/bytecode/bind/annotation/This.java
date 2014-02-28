package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.*;

/**
 * Parameters that are annotated with this annotation will be assigned a reference to the instrumented object, if
 * the instrumented method is not static. Otherwise, the method with this parameter annotation will be excluded from
 * the list of possible binding candidates of the static source method.
 *
 * @see com.blogspot.mydailyjava.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 * @see com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface This {

    /**
     * A binder for handling the
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.This}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    static enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<This> {
        INSTANCE;

        private static final int THIS_REFERENCE_INDEX = 0;

        @Override
        public Class<This> getHandledType() {
            return This.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(This annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               TypeDescription instrumentedType,
                                                               Assigner assigner) {
            TypeDescription targetType = target.getParameterTypes().get(targetParameterIndex);
            if (targetType.isPrimitive()) {
                throw new IllegalStateException(String.format("The %d. argument virtual %s is a primitive type " +
                        "and can never be bound to an instance", targetParameterIndex, target));
            } else if (targetType.isArray()) {
                throw new IllegalStateException(String.format("The %d. argument virtual %s is an array type " +
                        "and can never be bound to an instance", targetParameterIndex, target));
            } else if (source.isStatic()) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            boolean runtimeType = RuntimeType.Verifier.check(target, targetParameterIndex);
            StackManipulation stackManipulation = assigner.assign(instrumentedType, targetType, runtimeType);
            return new MethodDelegationBinder.ParameterBinding.Anonymous(
                    new StackManipulation.Compound(
                            MethodVariableAccess.OBJECT_REFERENCE.loadFromIndex(THIS_REFERENCE_INDEX),
                            stackManipulation));
        }
    }
}
