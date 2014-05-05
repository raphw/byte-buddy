package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.This;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationThisTest extends AbstractInstrumentationTest {

    @Test
    public void testThis() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(Bar.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), is((Object) instance));
    }

    public static class Foo {

        public Object foo() {
            return null;
        }
    }

    public static class Bar {

        public static Object qux(@This Foo foo) {
            return foo;
        }

        public static Object baz(@This Void v) {
            return v;
        }
    }
}
