package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bind.annotation.IgnoreForBinding;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationIgnoreForBindingTest extends AbstractImplementationTest {

    private static final String FOO = "FOO", BAR = "bar";

    @Test
    public void testIgnoreForBinding() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(Bar.class));
        Foo instance = loaded.getLoaded().getConstructor().newInstance();
        assertThat(instance.foo(), is(FOO));
    }

    public static class Foo {

        public String foo() {
            return null;
        }
    }

    public static class Bar {

        public static String bar() {
            return FOO;
        }

        @IgnoreForBinding
        public static String qux() {
            return BAR;
        }
    }
}
