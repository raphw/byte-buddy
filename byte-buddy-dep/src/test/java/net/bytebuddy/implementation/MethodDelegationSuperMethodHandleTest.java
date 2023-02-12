package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperMethod;
import net.bytebuddy.implementation.bind.annotation.SuperMethodHandle;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.test.utility.AccessControllerRule;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationSuperMethodHandleTest {

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.v8.SingleDefaultMethodInterface";

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public MethodRule accessControllerRule = new AccessControllerRule();

    @Test
    @JavaVersionRule.Enforce(value = 7, target = Foo.class)
    public void testRunnableSuperCall() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(SampleClass.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.value, is(BAR));
        instance.foo();
        assertThat(instance.value, is(FOO));
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
    public void testAbstractMethodNonBindable() throws Exception {
        new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(MethodDelegation.to(SampleClass.class))
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

    public static class SampleClass {

        public static void foo(@SuperMethodHandle Object handle, @This Object target) throws Exception {
            Method method = Class.forName(JavaType.METHOD_HANDLE.getTypeStub().getName()).getMethod("invokeWithArguments", List.class);
            method.invoke(handle, Collections.singletonList(target));
        }
    }

    public static class Bar {

        public String bar() {
            return FOO;
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

    public static class IllegalAnnotation {

        public static String bar(@SuperMethodHandle String value) throws Exception {
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class NoFallback {

        @RuntimeType
        public static Object foo(@SuperMethodHandle(fallbackToDefault = false) Object handle) throws Exception {
            return null;
        }
    }
}
