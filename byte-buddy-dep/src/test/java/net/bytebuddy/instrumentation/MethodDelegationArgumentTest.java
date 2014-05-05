package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MethodDelegationArgumentTest extends AbstractInstrumentationTest {

    private static final String FOO = "bar", QUX = "qux", BAZ = "baz";
    private static final int BAR = 42;

    @Test
    public void testArgument() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(Bar.class));
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
