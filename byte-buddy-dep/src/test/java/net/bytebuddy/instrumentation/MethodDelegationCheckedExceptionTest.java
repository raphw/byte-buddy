package net.bytebuddy.instrumentation;

import org.junit.Test;

public class MethodDelegationCheckedExceptionTest extends AbstractInstrumentationTest {

    @SuppressWarnings("unused")
    public static class Foo {

        public void bar() {
            /* do nothing */
        }

        public static void doThrow() throws Exception {
            throw new Exception();
        }
    }

    @Test(expected = Exception.class)
    public void testUndeclaredCheckedException() throws Exception {
        instrument(Foo.class, MethodDelegation.to(Foo.class))
                .getLoaded()
                .newInstance()
                .bar();
    }
}
