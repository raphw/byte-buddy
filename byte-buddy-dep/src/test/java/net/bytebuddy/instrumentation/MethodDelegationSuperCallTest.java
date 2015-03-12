package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.PrecompiledTypeClassLoader;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationSuperCallTest extends AbstractInstrumentationTest {

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    private static final String CONFLICTING_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodConflictingInterface";

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new PrecompiledTypeClassLoader(getClass().getClassLoader());
    }

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

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodFallback() throws Exception {
        DynamicType.Loaded<?> loaded = instrument(Object.class,
                MethodDelegation.to(NonVoidTarget.class),
                classLoader,
                isMethod().and(not(isDeclaredBy(Object.class))),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD));
        Object instance = loaded.getLoaded().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodFallbackDisabled() throws Exception {
        instrument(Object.class,
                MethodDelegation.to(NoFallback.class),
                classLoader,
                isMethod().and(not(isDeclaredBy(Object.class))),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodFallbackAmbiguous() throws Exception {
        instrument(Object.class,
                MethodDelegation.to(NonVoidTarget.class),
                classLoader,
                isMethod().and(not(isDeclaredBy(Object.class))),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD),
                classLoader.loadClass(CONFLICTING_INTERFACE));
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

    public static class NoFallback {

        @RuntimeType
        public static Object foo(@SuperCall(fallbackToDefault = false) Callable<?> zuper) throws Exception {
            return null;
        }
    }
}
