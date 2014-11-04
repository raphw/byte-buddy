package net.bytebuddy.instrumentation;

import net.bytebuddy.test.packaging.PackagePrivateMethod;
import org.junit.Test;

public class MethodDelegationExceptionTest extends AbstractInstrumentationTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNoMethod() throws Exception {
        instrument(Foo.class, MethodDelegation.to(Bar.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoVisibleMethod() throws Exception {
        instrument(Foo.class, MethodDelegation.to(new PackagePrivateMethod()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoCompatibleMethod() throws Exception {
        instrument(Foo.class, MethodDelegation.to(Qux.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInterface() throws Exception {
        MethodDelegation.to(Runnable.class);
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
