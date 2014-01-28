package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.MethodDelegation;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation.Argument;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation.This;
import org.junit.Test;

import static com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodMatchers.named;

public class ByteBuddyTest {

    public static class Foo {

        public String test(String x, int y) {
            return "Foo: " + x + y;
        }
    }

    public static class Bar extends Foo {

        @Override
        public String test(String x, int y) {
            return "Bar: " + x + y;
        }

        public Integer foo(Integer i) {
            return i;
        }

        @Override
        public String toString() {
            return "{Bar}";
        }
    }

    public static class Delegate {

        public static String test(@Argument(1) int y, @Argument(0) Object x, @This Bar bar) {
            return "Interception: " + x + y + " " + bar.toString();
        }

        public static int x(int i) {
            return i * 2;
        }
    }

    @Test
    public void example() throws Exception {
        Bar object = ByteBuddy.make()
                .withAppendedClassVisitorWrapper(new DebuggingWrapper(System.out))
                .subclass(Bar.class)
                .method(named("test")).intercept(MethodDelegation.to(Delegate.class))
                .method(named("foo")).intercept(MethodDelegation.to(Delegate.class))
                .make()
                .load(getClass().getClassLoader())
                .newInstance();
        System.out.println(object.test("a", 10));
        System.out.println(object.foo(10));

    }


    @Test
    public void testName() throws Exception {
        System.out.println(String.class.isAssignableFrom(Object.class));
    }
}
