package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.auxiliary.MethodCallProxy;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

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
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SuperCall {

    /**
     * Determines if the generated proxy should be {@link java.io.Serializable}.
     *
     * @return {@code true} if the generated proxy should be {@link java.io.Serializable}.
     */
    boolean serializableProxy() default false;

    /**
     * Determines if the injected proxy should invoke the default method to the intercepted method if a common
     * super method invocation is not applicable. For this to be possible, the default method must not be ambiguous.
     *
     * @return {@code true} if the invocation should fall back to invoking the default method.
     */
    boolean fallbackToDefault() default true;

    /**
     * A binder for handling the
     * {@link net.bytebuddy.implementation.bind.annotation.SuperCall}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<SuperCall> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Class<SuperCall> getHandledType() {
            return SuperCall.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<SuperCall> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner) {
            TypeDescription targetType = target.getType().asErasure();
            if (!targetType.represents(Runnable.class) && !targetType.represents(Callable.class) && !targetType.represents(Object.class)) {
                throw new IllegalStateException("A super method call proxy can only be assigned to Runnable or Callable types: " + target);
            } else if (source.isConstructor()) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            Implementation.SpecialMethodInvocation specialMethodInvocation = annotation.loadSilent().fallbackToDefault()
                    ? implementationTarget.invokeDominant(source.asSignatureToken())
                    : implementationTarget.invokeSuper(source.asSignatureToken());
            return specialMethodInvocation.isValid()
                    ? new MethodDelegationBinder.ParameterBinding.Anonymous(new MethodCallProxy
                    .AssignableSignatureCall(specialMethodInvocation, annotation.loadSilent().serializableProxy()))
                    : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
        }

        @Override
        public String toString() {
            return "SuperCall.Binder." + name();
        }
    }
}
