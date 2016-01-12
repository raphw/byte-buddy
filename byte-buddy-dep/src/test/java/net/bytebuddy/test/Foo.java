package net.bytebuddy.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedParameterizedType;

public class Foo<T> {

    @Sample(1) Foo<@Sample(2) Void>.@Sample(3) Bar<@Sample(4) Void> field;

    public static void main(String[] args) throws Exception{
        System.out.println(Foo.class.getDeclaredField("field").getAnnotatedType().isAnnotationPresent(Sample.class));
        System.out.println(Foo.class.getDeclaredField("field").getAnnotatedType() instanceof AnnotatedParameterizedType);
        AnnotatedParameterizedType annotatedParameterizedType = (AnnotatedParameterizedType) Foo.class.getDeclaredField("field").getAnnotatedType();
        annotatedParameterizedType.getAnnotatedActualTypeArguments();
    }

    class Bar<S> { }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@interface Sample {
    int value();
}