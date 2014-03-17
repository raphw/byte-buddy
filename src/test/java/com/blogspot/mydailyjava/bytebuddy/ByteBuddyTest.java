package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.dynamic.ClassLoadingStrategy;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.MethodDelegation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.Super;
import org.junit.Test;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.isDeclaredBy;

public class ByteBuddyTest {

    public static class Foo {

        public Object bar() {
            return "abc";
        }
    }

    public static class Xyz extends Foo {

        @Override
        public String bar() {
            return "bcd";
        }
    }

    public static class Qux {

        public static String baz(@Super Foo foo) {
            return foo.bar().toString();
        }
    }

    @Test
    public void testName() throws Exception {
        Foo foo = new ByteBuddy().subclass(Xyz.class, ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .method(isDeclaredBy(Xyz.class)).intercept(MethodDelegation.to(Qux.class))
                .make().load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded().newInstance();
        System.out.println(foo.bar());
        Foo foo2 = new ByteBuddy().subclass(Foo.class, ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .method(isDeclaredBy(Foo.class)).intercept(MethodDelegation.to(Qux.class))
                .make().load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded().newInstance();
        System.out.println(foo2.bar());
    }
}
