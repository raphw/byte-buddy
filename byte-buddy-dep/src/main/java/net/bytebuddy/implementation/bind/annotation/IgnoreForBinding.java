package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;

import java.lang.annotation.*;

/**
 * Indicates that a given target method should never be considered for binding to a source method.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IgnoreForBinding {

    /**
     * A non-instantiable type that allows to check if a method should be ignored for binding.
     */
    final class Verifier {

        /**
         * As this is merely a utility method, the constructor is not supposed to be invoked.
         */
        private Verifier() {
            throw new UnsupportedOperationException();
        }

        /**
         * Validates if a method should be ignored for binding.
         *
         * @param methodDescription The method to validate.
         * @return {@code true} if the method should not be considered for binding.
         */
        public static boolean check(MethodDescription methodDescription) {
            return methodDescription.getDeclaredAnnotations().isAnnotationPresent(IgnoreForBinding.class);
        }
    }
}
