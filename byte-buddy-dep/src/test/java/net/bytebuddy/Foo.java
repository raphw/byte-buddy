package net.bytebuddy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;

public class Foo<T, @Bar S extends @Qux Callable<@Baz ?>> {

}

@Target(ElementType.TYPE_PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@interface Bar {
}


@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@interface Qux {
}


@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@interface Baz {
}
