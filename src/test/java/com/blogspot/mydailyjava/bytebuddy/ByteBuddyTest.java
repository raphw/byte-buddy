package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.SuperClassDelegation;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation.Argument;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation.This;
import org.junit.Test;

import java.util.Arrays;

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
    }

    public static class Delegate {

        public static String intercept(@Argument(0) String x) {
            return "Interception: " + x;
        }

//        public static String intercept(@Argument(0) String x, @Argument(1) int y) {
//            return "Interception: " + x + y;
//        }

        public static String test(@Argument(0) String x, @Argument(1) int y, @This Bar bar) {
            return "Interception + test: " + x + y + " " + bar.toString();
        }
    }

    @Test
    public void testSubclass() throws Exception {
        Bar object = ByteBuddy.make()
//                .withAppendedClassVisitorWrapper(new DebuggingWrapper(System.out))
                .subclass(Bar.class)
                .method(named("test")).intercept(SuperClassDelegation.INSTANCE)
//                .method(returns(String.class).and(takesArguments(String.class, int.class))).intercept(MethodDelegation.to(Delegate.class))
                .make()
                .load(getClass().getClassLoader())
                .newInstance();
        System.out.println(Arrays.asList(object.getClass().getDeclaredMethods()));
//        System.out.println(object.getClass());
//        System.out.println(object.getClass().getSuperclass());
        System.out.println(object.test("a", 10));

    }
}
