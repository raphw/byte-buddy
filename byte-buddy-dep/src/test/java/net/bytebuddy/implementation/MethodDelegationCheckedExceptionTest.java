package net.bytebuddy.implementation;

import org.junit.Test;

public class MethodDelegationCheckedExceptionTest extends AbstractImplementationTest {

    @Test(expected = Exception.class)
    public void testUndeclaredCheckedException() throws Exception {
        implement(Foo.class, MethodDelegation.to(Foo.class))
                .getLoaded()
                .getConstructor()
                .newInstance()
                .bar();
    }

    @SuppressWarnings("unused")
    public static class Foo {

        public static void doThrow() throws Exception {
            throw new Exception();
        }

        public void bar() {
            /* do nothing */
        }
    }
}
