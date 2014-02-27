package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;

import java.lang.annotation.*;

/**
 * Indicates that a given target method should never be considered for binding to a source method.
 *
 * @see com.blogspot.mydailyjava.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface IgnoreForBinding {

    /**
     * A non-instantiable type that allows to check if a method should be ignored for binding.
     */
    static final class Verifier {

        /**
         * Validates if a method should be ignored for binding.
         *
         * @param methodDescription The method to validate.
         * @return {@code true} if the method should not be considered for binding.
         */
        public static boolean check(MethodDescription methodDescription) {
            return methodDescription.isAnnotationPresent(IgnoreForBinding.class);
        }

        private Verifier() {
            throw new AssertionError();
        }
    }
}
