package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;

import java.lang.annotation.*;

/**
 * Defines a binding priority for a target method. If two target methods can be bound to a source method,
 * the one with the higher priority will be selected.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface BindingPriority {

    /**
     * The default priority for methods not carrying the
     * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.BindingPriority}
     * annotation.
     */
    static final double DEFAULT = 1d;

    /**
     * The binding priority for the annotated method. A method of higher priority will be preferred over a method
     * of lower priority.
     *
     * @return The priority for the annotated method.
     */
    double value();

    /**
     * An ambiguity resolver that considers the priority of a method as defined by the
     * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.BindingPriority}
     * annotation.
     */
    static enum Resolver implements MethodDelegationBinder.AmbiguityResolver {

        /**
         * The singleton instance.
         */
        INSTANCE;

        private static double resolve(BindingPriority bindingPriority) {
            return bindingPriority == null ? DEFAULT : bindingPriority.value();
        }

        @Override
        public Resolution resolve(MethodDescription source,
                                  MethodDelegationBinder.MethodBinding left,
                                  MethodDelegationBinder.MethodBinding right) {
            double leftPriority = resolve(left.getTarget().getAnnotation(BindingPriority.class));
            double rightPriority = resolve(right.getTarget().getAnnotation(BindingPriority.class));
            if (leftPriority == rightPriority) {
                return Resolution.AMBIGUOUS;
            } else if (leftPriority < rightPriority) {
                return Resolution.RIGHT;
            } else {
                return Resolution.LEFT;
            }
        }
    }
}
