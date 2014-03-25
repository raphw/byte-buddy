package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;

import java.lang.annotation.*;

/**
 * Parameters that are annotated with this annotation will be assigned by also considering the runtime type of the
 * target parameter. The same is true for a method's return type if a target method is annotated with this annotation.
 * <p>&nbsp;</p>
 * For example, if a source method {@code foo(@RuntimeType Object)} is attempted to be bound to
 * {@code bar(@RuntimeType String)}, the binding will attempt to cast the argument of {@code foo} to a {@code String}
 * type before calling {@code bar} with this argument. If this is not possible, a {@link java.lang.ClassCastException}
 * will be thrown at runtime.
 *
 * @see com.blogspot.mydailyjava.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface RuntimeType {

    /**
     * A non-instantiable type that allows to check if a method or parameter should consider a runtime type.
     */
    static final class Verifier {

        /**
         * Checks if method return values should be assigned by considering the run time type.
         *
         * @param methodDescription The method of interest.
         * @return {@code true} if the runtime type should be considered for binding the method's return value.
         */
        public static boolean check(MethodDescription methodDescription) {
            for (Annotation annotation : methodDescription.getAnnotations()) {
                if (annotation.annotationType() == RuntimeType.class) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Checks if a method parameter should be assigned by considering the run time type.
         *
         * @param methodDescription The method of interest.
         * @param parameterIndex    The index of the parameter of interest.
         * @return {@code true} if the runtime type should be considered for binding this parameter.
         */
        public static boolean check(MethodDescription methodDescription, int parameterIndex) {
            for (Annotation annotation : methodDescription.getParameterAnnotations()[parameterIndex]) {
                if (annotation.annotationType() == RuntimeType.class) {
                    return true;
                }
            }
            return false;
        }

        private Verifier() {
            throw new AssertionError();
        }
    }
}
