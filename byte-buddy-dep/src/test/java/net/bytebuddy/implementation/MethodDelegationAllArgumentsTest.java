package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationAllArgumentsTest {

    private static final int FOO = 42, BAR = 21;

    private static final String QUX = "qux", BAZ = "baz", FOOBAR = "foobar";

    @Test
    public void testStrictBindable() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(Bar.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(FOO, BAR), is((Object) (QUX + FOO + BAR)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStrictNonBindableThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Qux.class).method(isDeclaredBy(Qux.class))
                .intercept(MethodDelegation.to(BazStrict.class))
                .make();
    }

    @Test
    public void testSlackNonBindable() throws Exception {
        DynamicType.Loaded<Qux> loaded = new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(MethodDelegation.to(BazSlack.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Qux instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(FOOBAR, BAZ), is((Object) (QUX + BAZ)));
    }

    @Test
    public void testIncludeSelf() throws Exception {DynamicType.Loaded<Qux> loaded = new ByteBuddy()
            .subclass(Qux.class)
            .method(isDeclaredBy(Qux.class))
            .intercept(MethodDelegation.to(IncludeSelf.class))
            .make()
            .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Qux instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
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
