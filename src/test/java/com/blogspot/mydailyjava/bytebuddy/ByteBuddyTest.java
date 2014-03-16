package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.dynamic.ClassLoadingStrategy;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.MethodDelegation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.Super;
import org.junit.Test;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.isDeclaredBy;

public class ByteBuddyTest {

    public static class Foo {

        public Foo(String s) {
            System.out.println("Constructor: " + s);
        }

        public void fooBase() {
            System.out.println("foo base");
        }
    }

    public static class Bar {

        public static void bar(@Super(strategy = Super.Instantiation.CONSTRUCTOR, constructorArguments = {String.class}) Foo foo) {
            System.out.println("bar interception");
            foo.fooBase();
        }
    }

    @Test
    public void testName() throws Exception {
        new ByteBuddy().subclass(Foo.class).method(isDeclaredBy(Foo.class)).intercept(MethodDelegation.to(Bar.class)).make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded().getDeclaredConstructor(String.class).newInstance("hello").fooBase();
    }
}
