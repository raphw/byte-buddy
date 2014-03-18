package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MethodDelegationRuntimeTypeTest extends AbstractInstrumentationTest {

    private static final String FOO = "FOO";

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

    @Test
    public void testRuntimeType() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(Bar.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(FOO), is(FOO));
    }
}
