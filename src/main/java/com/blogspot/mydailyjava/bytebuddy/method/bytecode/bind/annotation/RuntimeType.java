package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface RuntimeType {

    static final class Verifier {

        public static boolean check(MethodDescription methodDescription) {
            for (Annotation annotation : methodDescription.getAnnotations()) {
                if (annotation.annotationType() == RuntimeType.class) {
                    return true;
                }
            }
            return false;
        }

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
