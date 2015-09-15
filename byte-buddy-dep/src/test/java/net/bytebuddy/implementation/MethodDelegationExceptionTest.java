package net.bytebuddy.implementation;

import net.bytebuddy.test.packaging.PackagePrivateMethod;
import org.junit.Test;

public class MethodDelegationExceptionTest extends AbstractImplementationTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNoMethod() throws Exception {
        implement(Foo.class, MethodDelegation.to(Bar.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoVisibleMethod() throws Exception {
        implement(Foo.class, MethodDelegation.to(new PackagePrivateMethod()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoCompatibleMethod() throws Exception {
        implement(Foo.class, MethodDelegation.to(Qux.class));
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
