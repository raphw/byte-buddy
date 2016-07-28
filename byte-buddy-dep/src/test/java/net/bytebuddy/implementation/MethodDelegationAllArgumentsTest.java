package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationAllArgumentsTest extends AbstractImplementationTest {

    private static final int FOO = 42, BAR = 21;

    private static final String QUX = "qux", BAZ = "baz", FOOBAR = "foobar";

    @Test
    public void testStrictBindable() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(Bar.class));
        Foo instance = loaded.getLoaded().getConstructor().newInstance();
        assertThat(instance.foo(FOO, BAR), is((Object) (QUX + FOO + BAR)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStrictNonBindableThrowsException() throws Exception {
        implement(Qux.class, MethodDelegation.to(BazStrict.class));
    }

    @Test
    public void testSlackNonBindable() throws Exception {
        DynamicType.Loaded<Qux> loaded = implement(Qux.class, MethodDelegation.to(BazSlack.class));
        Qux instance = loaded.getLoaded().getConstructor().newInstance();
        assertThat(instance.foo(FOOBAR, BAZ), is((Object) (QUX + BAZ)));
    }

    @Test
    public void testIncludeSelf() throws Exception {
        DynamicType.Loaded<Qux> loaded = implement(Qux.class, MethodDelegation.to(IncludeSelf.class));
        Qux instance = loaded.getLoaded().getConstructor().newInstance();
        assertThat(instance.foo(QUX, BAZ), is((Object) instance));
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
            assertThat(args.length, is(2));
            return QUX + args[0] + args[1];
        }
    }

    public static class BazSlack {

        public static String qux(@AllArguments(AllArguments.Assignment.SLACK) String[] args) {
            assertThat(args.length, is(1));
            return QUX + args[0];
        }
    }

    public static class IncludeSelf {

        public static Object intercept(@AllArguments(includeSelf = true) Object[] args) {
            assertThat(args.length, is(3));
            assertThat(args[1], is((Object) QUX));
            assertThat(args[2], is((Object) BAZ));
            return args[0];
        }
    }
}
