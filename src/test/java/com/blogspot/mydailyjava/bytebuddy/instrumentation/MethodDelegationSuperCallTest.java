package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall;
import org.junit.Test;

import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationSuperCallTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar";

    public static class Foo {

        public String value = BAR;

        public void foo() {
            value = FOO;
        }
    }

    public static class RunnableClass {

        public static void foo(@SuperCall Runnable runnable) {
            runnable.run();
        }
    }

    @Test
    public void testRunnableSuperCall() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(RunnableClass.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.value, is(BAR));
        instance.foo();
        assertThat(instance.value, is(FOO));
    }

    public static class Bar {

        public String bar() {
            return FOO;
        }
    }

    public static class CallableClass {

        public static String bar(@SuperCall Callable<String> callable) throws Exception {
            return callable.call();
        }
    }

    @Test
    public void testCallableSuperCall() throws Exception {
        DynamicType.Loaded<Bar> loaded = instrument(Bar.class, MethodDelegation.to(CallableClass.class));
        Bar instance = loaded.getLoaded().newInstance();
        assertThat(instance.bar(), is(FOO));
    }
}
