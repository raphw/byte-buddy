package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationBindingPriorityTest extends AbstractImplementationTest {

    private static final String FOO = "FOO", BAR = "bar";

    private static final int PRIORITY = 10;

    @Test
    public void testBindingPriority() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(Bar.class));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is(FOO));
    }

    public static class Foo {

        public String foo() {
            return null;
        }
    }

    public static class Bar {

        @BindingPriority(PRIORITY)
        public static String bar() {
            return FOO;
        }

        public static String qux() {
            return BAR;
        }
    }
}
