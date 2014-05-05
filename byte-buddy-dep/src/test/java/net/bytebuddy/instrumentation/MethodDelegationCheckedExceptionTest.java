package net.bytebuddy.instrumentation;

import org.junit.Test;

public class MethodDelegationCheckedExceptionTest extends AbstractInstrumentationTest {

    @Test(expected = Exception.class)
    public void testUndeclaredCheckedException() throws Exception {
        instrument(Foo.class, MethodDelegation.to(Foo.class))
                .getLoaded()
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
