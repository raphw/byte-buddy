package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.test.packaging.PackagePrivateMethod;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

public class MethodDelegationExceptionTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNoMethod() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(Bar.class))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoVisibleMethod() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(new PackagePrivateMethod()))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoCompatibleMethod() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(Qux.class))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArray() throws Exception {
        MethodDelegation.to(int[].class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrimitive() throws Exception {
        MethodDelegation.to(int.class);
    }

    public static class Foo {

        public void bar() {
            /* do nothing */
        }
    }

    public static class Bar {
        /* empty */
    }

    public static class Qux {

        public static void foo(Object o) {
            /* do nothing */
        }
    }
}
