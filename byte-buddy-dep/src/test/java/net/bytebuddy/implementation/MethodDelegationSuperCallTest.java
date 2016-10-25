package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationSuperCallTest {

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    private static final String CONFLICTING_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodConflictingInterface";

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testRunnableSuperCall() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(RunnableClass.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.value, is(BAR));
        instance.foo();
        assertThat(instance.value, is(FOO));
    }

    @Test
    public void testCallableSuperCall() throws Exception {
        DynamicType.Loaded<Bar> loaded = new ByteBuddy()
                .subclass(Bar.class)
                .method(isDeclaredBy(Bar.class))
                .intercept(MethodDelegation.to(CallableClass.class))
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Bar instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.bar(), is(FOO));
    }

    @Test
    public void testVoidToNonVoidSuperCall() throws Exception {
        DynamicType.Loaded<VoidTest> loaded = new ByteBuddy()
                .subclass(VoidTest.class)
                .method(isDeclaredBy(VoidTest.class))
                .intercept(MethodDelegation.to(NonVoidTarget.class))
                .make()
                .load(VoidTest.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        VoidTest instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo();
        instance.assertOnlyCall(FOO);
    }

    @Test
    public void testRuntimeTypeSuperCall() throws Exception {
        DynamicType.Loaded<RuntimeTypeTest> loaded = new ByteBuddy()
                .subclass(RuntimeTypeTest.class)
                .method(isDeclaredBy(RuntimeTypeTest.class))
                .intercept(MethodDelegation.to(RuntimeTypeTarget.class))
                .make()
                .load(RuntimeTypeTest.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        RuntimeTypeTest instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is(FOO));
    }

    @Test
    public void testSerializableProxy() throws Exception {
        DynamicType.Loaded<Bar> loaded = new ByteBuddy()
                .subclass(Bar.class)
                .method(isDeclaredBy(Bar.class))
                .intercept(MethodDelegation.to(SerializationCheck.class))
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Bar instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.bar(), is(FOO));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodFallback() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD))
                .intercept(MethodDelegation.to(NonVoidTarget.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodFallbackDisabled() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD))
                .intercept(MethodDelegation.to(NoFallback.class))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodFallbackAmbiguous() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE))
                .intercept(MethodDelegation.to(NonVoidTarget.class))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAbstractMethodNonBindable() throws Exception {
        new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(MethodDelegation.to(CallableClass.class))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongTypeThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Bar.class)
                .method(isDeclaredBy(Bar.class))
                .intercept(MethodDelegation.to(IllegalAnnotation.class))
                .make();
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

    public abstract static class Qux {

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

    @SuppressWarnings("unused")
    public static class NoFallback {

        @RuntimeType
        public static Object foo(@SuperCall(fallbackToDefault = false) Callable<?> zuper) throws Exception {
            return null;
        }
    }
}
