package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.MethodDelegation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.AllArguments;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.This;
import com.blogspot.mydailyjava.bytebuddy.proxy.DynamicType;
import org.junit.Test;

import java.util.Arrays;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.named;

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

        public Integer foo(Object i, int y) {
            return y;
        }

        public void test(String a) {

        }

        @Override
        public String toString() {
            return "{Bar}";
        }
    }

    public static class Delegate {

        public static String test(@Argument(1) int y, @Argument(0) Object x, @This Bar bar, @AllArguments Object[] objects) {
            return "Interception: " + x + y + " " + bar.toString() + Arrays.asList(objects);
        }

        public static int x(@AllArguments @RuntimeType int[] i) {
            for (int in : i) {
                System.out.println(in);
            }
            return i.length;
        }

        public static void test0(String a) {
            System.out.println("test0");
        }
    }

    @Test
    public void demonstratingExampleToBeRemoved() throws Exception {
        Bar object = ByteBuddy.make()
//                .withAppendedClassVisitorWrapper(new DebuggingWrapper(System.out))
                .subclass(Bar.class)
                .method(named("test")).intercept(MethodDelegation.to(Delegate.class))
                .method(named("foo")).intercept(MethodDelegation.to(Delegate.class))
                .method(named("test")).intercept(MethodDelegation.to(Delegate.class))
                .make()
                .load(getClass().getClassLoader(), DynamicType.ClassLoadingStrategy.WRAPPER)
                .getMainType()
                .newInstance();
        System.out.println(object.test("a", 10));
        System.out.println(object.foo(10, 3));
        object.test("");
    }

    @Test
    public void testName() throws Exception {
        System.out.println(String.class.isAssignableFrom(Object.class));
    }
}
