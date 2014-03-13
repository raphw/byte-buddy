package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy;

import java.lang.annotation.*;
import java.util.concurrent.Callable;

/**
 * Parameters that are annotated with this annotation will be assigned a proxy for calling the instrumented method's
 * {@code super} implementation. If a method does not have a super instrumentation, the method with this parameter
 * annotation will not be considered as a possible binding candidate for the source method without such a super
 * implementation.
 * <p/>
 * The proxy will both implement the {@link java.util.concurrent.Callable} and the {@link java.lang.Runnable} interfaces
 * such that the annotated parameter must be assignable to any of those interfaces or be of the {@link java.lang.Object}
 * type.
 *
 * @see com.blogspot.mydailyjava.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 * @see com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface SuperCall {

    /**
     * A binder for handling the
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    static enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<SuperCall> {
        INSTANCE;

        @Override
        public Class<SuperCall> getHandledType() {
            return SuperCall.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(SuperCall annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               TypeDescription instrumentedType,
                                                               Assigner assigner) {
            TypeDescription targetType = target.getParameterTypes().get(targetParameterIndex);
            if (!targetType.represents(Runnable.class) && !targetType.represents(Callable.class) && !targetType.represents(Object.class)) {
                throw new IllegalStateException("A method call proxy can only be assigned to Runnable or Callable types: " + target);
            } else if (target.isAbstract()) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            } else {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new MethodCallProxy.AssignableSignatureCall(source));
            }
        }
    }
}
