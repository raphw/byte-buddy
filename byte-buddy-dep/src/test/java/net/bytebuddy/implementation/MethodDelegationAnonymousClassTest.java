package net.bytebuddy.implementation;

import org.junit.Test;

public class MethodDelegationAnonymousClassTest extends AbstractImplementationTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test(expected = IllegalStateException.class)
    public void testAnonymousClassDelegationThrowsException() throws Exception {
        implement(Foo.class, MethodDelegation.to(new Object() {
            @SuppressWarnings("unused")
            public String intercept() {
                return BAR;
            }
        })).getLoaded().newInstance().foo();
    }

    public static class Foo {

        public String foo() {
            return FOO;
        }
    }
}
