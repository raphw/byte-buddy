package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.AllArguments;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MethodDelegationAllArgumentsTest extends AbstractInstrumentationTest {

    private static final int FOO = 42, BAR = 21;
    private static final String QUX = "qux", BAZ = "baz", FOOBAR = "foobar";

    @Test
    public void testStrictBindable() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(Bar.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(FOO, BAR), is((Object) (QUX + FOO + BAR)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStrictNonBindableThrowsException() throws Exception {
        instrument(Qux.class, MethodDelegation.to(BazStrict.class));
    }

    @Test
    public void testSlackNonBindable() throws Exception {
        DynamicType.Loaded<Qux> loaded = instrument(Qux.class, MethodDelegation.to(BazSlack.class));
        Qux instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(FOOBAR, BAZ), is((Object) (QUX + BAZ)));
    }

    public static class Foo {

        public Object foo(int i1, Integer i2) {
            return null;
        }
    }

    public static class Bar {

        public static String qux(@AllArguments int[] args) {
            return QUX + args[0] + args[1];
        }
    }

    public static class Qux {

        public Object foo(Object o, String s) {
            return null;
        }
    }

    public static class BazStrict {

        public static String qux(@AllArguments String[] args) {
            return QUX + args[0] + args[1];
        }
    }

    public static class BazSlack {

        public static String qux(@AllArguments(AllArguments.Assignment.SLACK) String[] args) {
            return QUX + args[0];
        }
    }
}
