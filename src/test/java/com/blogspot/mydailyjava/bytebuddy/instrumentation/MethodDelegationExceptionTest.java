package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.utility.PackagePrivateSample;
import org.junit.Test;

public class MethodDelegationExceptionTest extends AbstractInstrumentationTest {

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

    @Test(expected = IllegalArgumentException.class)
    public void testNoMethod() throws Exception {
        instrument(Foo.class, MethodDelegation.to(Bar.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testNoVisibleMethod() throws Exception {
        instrument(Foo.class, MethodDelegation.to(new PackagePrivateSample()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoCompatibleMethod() throws Exception {
        instrument(Foo.class, MethodDelegation.to(Qux.class));
    }
}
