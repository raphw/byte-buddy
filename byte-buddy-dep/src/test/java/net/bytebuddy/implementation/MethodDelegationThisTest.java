package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationThisTest extends AbstractImplementationTest {

    @Test
    public void testThis() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(Bar.class));
        Foo instance = loaded.getLoaded().getConstructor().newInstance();
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
