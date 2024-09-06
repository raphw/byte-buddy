package net.bytebuddy.test.precompiled.v11;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;

public class ClassExtendsTypeReference {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public @interface TypeAnnotation {
        /* empty */
    }

    public Object foo() {
        return new ArrayList<@TypeAnnotation Object>() {
            /* empty */
        };
    }
}
