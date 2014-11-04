package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.TypeProxy;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Default {

    boolean serializableProxy() default false;

    static enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Default> {

        INSTANCE;

        @Override
        public Class<Default> getHandledType() {
            return Default.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Default> annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            TypeDescription parameterType = target.getParameterTypes().get(targetParameterIndex);
            if (!parameterType.isInterface()) {
                throw new IllegalStateException(target + " uses the @Default annotation on a non-interface type");
            } else if (source.isStatic() || !instrumentationTarget.getTypeDescription().isAssignableTo(parameterType)) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            } else {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new TypeProxy.ForDefaultMethod(parameterType,
                        instrumentationTarget,
                        annotation.load().serializableProxy()));
            }
        }
    }
}

