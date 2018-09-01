package net.bytebuddy;

import net.bytebuddy.implementation.InvokeDynamic;
import net.bytebuddy.test.utility.DebuggingWrapper;

import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.named;

public interface Foo {

    static Foo make(String y) {
        return x -> x + y;
    }

    String apply(String arg);

    static String imp(String value) {
        return value;
    }

    static void main(String[] args) throws Exception {
        Method target = Foo.class.getMethod("imp", String.class);

        Factory factory = new ByteBuddy()
                .subclass(Factory.class)
                .visit(DebuggingWrapper.makeDefault())
                .method(named("make"))
                .intercept(InvokeDynamic.lambda(target, Foo.class))
                .make()
                .load(Foo.class.getClassLoader())
                .getLoaded()
                .newInstance();

        Foo foo = factory.make("bar");
        String result = foo.apply("foo");
        System.out.println(result);
    }

    interface Factory {

        Foo make(String x);
    }
}