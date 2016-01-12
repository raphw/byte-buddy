package net.bytebuddy.description;

import jdk.internal.org.objectweb.asm.TypeReference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

public class Foo<T,
        S extends @Sample T,
        U extends @Sample Runnable,
        V extends @Sample String,
        W extends @Sample List<?>,
        X extends @Sample ArrayList<?>> {

    public static void main(String[] args) {
        System.out.println(Foo.class.getTypeParameters()[1].getAnnotatedBounds()[0].isAnnotationPresent(Sample.class));
        System.out.println(Foo.class.getTypeParameters()[2].getAnnotatedBounds()[0].isAnnotationPresent(Sample.class));
        System.out.println(Foo.class.getTypeParameters()[3].getAnnotatedBounds()[0].isAnnotationPresent(Sample.class));
        System.out.println(Foo.class.getTypeParameters()[4].getAnnotatedBounds()[0].isAnnotationPresent(Sample.class));
        System.out.println(Foo.class.getTypeParameters()[5].getAnnotatedBounds()[0].isAnnotationPresent(Sample.class));

        System.out.println(new TypeReference(285278208).getTypeParameterBoundIndex());
        System.out.println(new TypeReference(285344000).getTypeParameterBoundIndex());
        System.out.println(new TypeReference(285409280).getTypeParameterBoundIndex());
        System.out.println(new TypeReference(285475072).getTypeParameterBoundIndex());
        System.out.println(new TypeReference(285540352).getTypeParameterBoundIndex());
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@interface Sample { }
