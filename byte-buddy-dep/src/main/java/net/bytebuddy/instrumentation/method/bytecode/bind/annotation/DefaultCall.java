package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy;

import java.lang.annotation.*;
import java.util.concurrent.Callable;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DefaultCall {

    Class<?> targetType() default void.class;

    static enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<DefaultCall> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Class<DefaultCall> getHandledType() {
            return DefaultCall.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(DefaultCall annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            TypeDescription targetType = target.getParameterTypes().get(targetParameterIndex);
            if (!targetType.represents(Runnable.class) && !targetType.represents(Callable.class) && !targetType.represents(Object.class)) {
                throw new IllegalStateException("A default method call proxy can only be assigned to Runnable or Callable types: " + target);
            }
            Instrumentation.SpecialMethodInvocation specialMethodInvocation = locate(annotation.targetType()).resolve(instrumentationTarget, source);
            return specialMethodInvocation.isValid()
                    ? new MethodDelegationBinder.ParameterBinding.Anonymous(new MethodCallProxy.AssignableSignatureCall(specialMethodInvocation))
                    : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
        }

        private static MethodLocator locate(Class<?> type) {
            return type == void.class
                    ? MethodLocator.Implicit.INSTANCE
                    : new MethodLocator.Explicit(type);
        }

        private static interface MethodLocator {

            static enum Implicit implements MethodLocator {

                INSTANCE;

                @Override
                public Instrumentation.SpecialMethodInvocation resolve(Instrumentation.Target instrumentationTarget,
                                                                       MethodDescription source) {
                    String uniqueSignature = source.getUniqueSignature();
                    Instrumentation.SpecialMethodInvocation specialMethodInvocation = null;
                    for (TypeDescription candidate : instrumentationTarget.getTypeDescription().getInterfaces()) {
                        if (source.isSpecializableFor(candidate)) {
                            if (specialMethodInvocation != null) {
                                return Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE;
                            }
                            specialMethodInvocation = instrumentationTarget.invokeDefault(candidate, uniqueSignature);
                        }
                    }
                    return specialMethodInvocation;
                }
            }

            static class Explicit implements MethodLocator {

                private final TypeDescription typeDescription;

                public Explicit(Class<?> type) {
                    typeDescription = new TypeDescription.ForLoadedType(type);
                }

                @Override
                public Instrumentation.SpecialMethodInvocation resolve(Instrumentation.Target instrumentationTarget,
                                                                       MethodDescription source) {
                    if (!typeDescription.isInterface()) {
                        throw new IllegalStateException(source + " method carries default method call parameter on non-interface type");
                    }
                    return instrumentationTarget.invokeDefault(typeDescription, source.getUniqueSignature());
                }
            }

            Instrumentation.SpecialMethodInvocation resolve(Instrumentation.Target instrumentationTarget,
                                                            MethodDescription source);
        }
    }
}
