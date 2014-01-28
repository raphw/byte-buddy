package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface IgnoreForBinding {

    static final class Verifier {

        public static boolean check(MethodDescription methodDescription) {
            for (Annotation annotation : methodDescription.getAnnotations()) {
                if (annotation.annotationType() == IgnoreForBinding.class) {
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
