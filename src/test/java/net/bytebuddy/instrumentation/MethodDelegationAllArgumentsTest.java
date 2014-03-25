package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.AllArguments;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MethodDelegationAllArgumentsTest extends AbstractInstrumentationTest {

    private static final int FOO = 42, BAR = 21;
    private static final String QUX = "qux", BAZ = "baz";

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

    @Test
    public void testAllArguments() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(Bar.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(FOO, BAR), is((Object) (QUX + FOO + BAR)));
    }
}
