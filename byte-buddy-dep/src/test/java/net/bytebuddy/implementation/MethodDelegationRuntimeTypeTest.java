package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationRuntimeTypeTest extends AbstractImplementationTest {

    private static final String FOO = "FOO";

    @Test
    public void testRuntimeType() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(Bar.class));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(FOO), is(FOO));
    }

    public static class Foo {

        public String foo(Object o) {
            return (String) o;
        }
    }

    public static class Bar {

        @RuntimeType
        public static Object foo(@RuntimeType String s) {
            return s;
        }
    }
}
