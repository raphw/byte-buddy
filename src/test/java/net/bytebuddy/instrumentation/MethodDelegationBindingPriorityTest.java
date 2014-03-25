package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.BindingPriority;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationBindingPriorityTest extends AbstractInstrumentationTest {

    private static final String FOO = "FOO", BAR = "bar";
    private static final double PRIORITY = 10d;

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

    @Test
    public void testBindingPriority() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(Bar.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), is(FOO));
    }
}
