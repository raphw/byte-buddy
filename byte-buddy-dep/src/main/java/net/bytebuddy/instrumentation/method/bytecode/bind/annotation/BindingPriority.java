package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;

import java.lang.annotation.*;

/**
 * Defines a binding priority for a target method. If two target methods can be bound to a source method,
 * the one with the higher priority will be selected.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
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

        /**
         * Resolves the explicitly stated binding priority of a method or returns the default value if no such
         * explicit information can be found.
         *
         * @param bindingPriority The annotation of the method or {@code null} if no such annotation was found.
         * @return The factual priority of the method under investigation.
         */
        private static double resolve(AnnotationDescription.Loadable<BindingPriority> bindingPriority) {
            return bindingPriority == null ? DEFAULT : bindingPriority.loadSilent().value();
        }

        @Override
        public Resolution resolve(MethodDescription source,
                                  MethodDelegationBinder.MethodBinding left,
                                  MethodDelegationBinder.MethodBinding right) {
            double leftPriority = resolve(left.getTarget().getDeclaredAnnotations().ofType(BindingPriority.class));
            double rightPriority = resolve(right.getTarget().getDeclaredAnnotations().ofType(BindingPriority.class));
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
