package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bind.annotation.Argument;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MethodDelegationArgumentTest extends AbstractImplementationTest {

    private static final String FOO = "bar", QUX = "qux", BAZ = "baz";

    private static final int BAR = 42;

    @Test
    public void testArgument() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(Bar.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(FOO, BAR), is((Object) (QUX + FOO + BAR)));
    }

    public static class Foo {

        public Object foo(String s, Integer i) {
            return null;
        }
    }

    public static class Bar {

        public static String qux(@Argument(1) Integer i, @Argument(0) String s) {
            return QUX + s + i;
        }

        public static String baz(String s, Object o) {
            return BAZ + s + o;
        }
    }
}
