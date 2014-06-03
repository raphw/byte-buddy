package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy;

import java.lang.annotation.*;
import java.util.concurrent.Callable;

/**
 * Parameters that are annotated with this annotation will be assigned a proxy for calling the instrumented method's
 * {@code super} implementation. If a method does not have a super implementation, calling the annotated proxy will
 * throw an exception.
 * <p>&nbsp;</p>
 * The proxy will both implement the {@link java.util.concurrent.Callable} and the {@link java.lang.Runnable} interfaces
 * such that the annotated parameter must be assignable to any of those interfaces or be of the {@link java.lang.Object}
 * type.
 *
 * @see net.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SuperCall {

    /**
     * A binder for handling the
     * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    static enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<SuperCall> {

        /**
         * The singleton instance.
         */
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
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            TypeDescription targetType = target.getParameterTypes().get(targetParameterIndex);
            if (!targetType.represents(Runnable.class) && !targetType.represents(Callable.class) && !targetType.represents(Object.class)) {
                throw new IllegalStateException("A super method call proxy can only be assigned to Runnable or Callable types: " + target);
            }
            Instrumentation.SpecialMethodInvocation specialMethodInvocation = instrumentationTarget.invokeSuper(source,
                    Instrumentation.Target.MethodLookup.Default.EXACT);
            return specialMethodInvocation.isValid()
                    ? new MethodDelegationBinder.ParameterBinding.Anonymous(new MethodCallProxy.AssignableSignatureCall(specialMethodInvocation))
                    : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
        }
    }
}
