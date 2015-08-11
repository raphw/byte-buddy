package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.DefaultValue;

import java.lang.annotation.*;

/**
 * Binds the parameter type's default value to the annotated parameter, i.e. {@code null} or a numeric value
 * representing zero.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Empty {

    /**
     * A binder for the {@link net.bytebuddy.implementation.bind.annotation.Empty} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Empty> {

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
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner) {
            return new MethodDelegationBinder.ParameterBinding.Anonymous(DefaultValue.of(target.getType().asErasure()));
        }

        @Override
        public String toString() {
            return "Empty.Binder." + name();
        }
    }
}
