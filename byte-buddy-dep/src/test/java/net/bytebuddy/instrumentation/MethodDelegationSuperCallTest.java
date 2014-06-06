package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall;
import net.bytebuddy.utility.CallTraceable;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.Serializable;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationSuperCallTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testRunnableSuperCall() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(RunnableClass.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.value, is(BAR));
        instance.foo();
        assertThat(instance.value, is(FOO));
    }

    @Test
    public void testCallableSuperCall() throws Exception {
        DynamicType.Loaded<Bar> loaded = instrument(Bar.class, MethodDelegation.to(CallableClass.class));
        Bar instance = loaded.getLoaded().newInstance();
        assertThat(instance.bar(), is(FOO));
    }

    @Test
    public void testVoidToNonVoidSuperCall() throws Exception {
        DynamicType.Loaded<VoidTest> loaded = instrument(VoidTest.class, MethodDelegation.to(NonVoidTarget.class));
        VoidTest instance = loaded.getLoaded().newInstance();
        instance.foo();
        instance.assertOnlyCall(FOO);
    }

    @Test
    public void testRuntimeTypeSuperCall() throws Exception {
        DynamicType.Loaded<RuntimeTypeTest> loaded = instrument(RuntimeTypeTest.class, MethodDelegation.to(RuntimeTypeTarget.class));
        RuntimeTypeTest instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), is(FOO));
    }

    @Test
    public void testSerializableProxy() throws Exception {
        DynamicType.Loaded<Bar> loaded = instrument(Bar.class, MethodDelegation.to(SerializationCheck.class));
        Bar instance = loaded.getLoaded().newInstance();
        assertThat(instance.bar(), is(FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAbstractMethodNonBindable() throws Exception {
        instrument(Qux.class, MethodDelegation.to(CallableClass.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongTypeThrowsException() throws Exception {
        instrument(Bar.class, MethodDelegation.to(IllegalAnnotation.class));
    }

    public static class Foo {

        public String value = BAR;

        public void foo() {
            value = FOO;
        }
    }

    public static class RunnableClass {

        public static void foo(@SuperCall Runnable runnable) {
            assertThat(runnable, CoreMatchers.not(instanceOf(Serializable.class)));
            runnable.run();
        }
    }

    public static class Bar {

        public String bar() {
            return FOO;
        }
    }

    public static class CallableClass {

        public static String bar(@SuperCall Callable<String> callable) throws Exception {
            assertThat(callable, CoreMatchers.not(instanceOf(Serializable.class)));
            return callable.call();
        }
    }

    public static abstract class Qux {

        public abstract String bar();
    }

    public static class VoidTest extends CallTraceable {

        public void foo() {
            register(FOO);
        }
    }

    public static class NonVoidTarget {

        @RuntimeType
        public static Object foo(@SuperCall Callable<?> zuper) throws Exception {
            return zuper.call();
        }
    }

    public static class RuntimeTypeTest {

        public String foo() {
            return FOO;
        }
    }

    public static class RuntimeTypeTarget {

        @RuntimeType
        public static Object foo(@SuperCall Callable<?> zuper) throws Exception {
            return zuper.call();
        }
    }

    public static class IllegalAnnotation {

        public static String bar(@SuperCall String value) throws Exception {
            return value;
        }
    }

    public static class SerializationCheck {

        public static String bar(@SuperCall(serializableProxy = true) Callable<String> callable) throws Exception {
            assertThat(callable, instanceOf(Serializable.class));
            return callable.call();
        }
    }
}
