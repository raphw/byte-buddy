package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.DefaultValue;

import java.lang.annotation.*;

/**
 * Binds the parameter type's default value to the annotated parameter, i.e. {@code null} or a numeric value
 * representing zero.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Empty {

    /**
     * A binder for the {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Empty} annotation.
     */
    static enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Empty> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Class<Empty> getHandledType() {
            return Empty.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Empty> annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            return new MethodDelegationBinder.ParameterBinding.Anonymous(DefaultValue
                    .of(target.getParameterTypes().get(targetParameterIndex)));
        }
    }
}
