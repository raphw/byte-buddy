package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperMethod;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationSuperMethodTest {

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
                .intercept(MethodDelegation.to(SampleClass.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.value, is(BAR));
        instance.foo();
        assertThat(instance.value, is(FOO));
    }

    @Test
    public void testRunnableSuperCallNoCache() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(SampleClassNoCache.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.value, is(BAR));
        instance.foo();
        assertThat(instance.value, is(FOO));
    }

    @Test
    public void testRunnableSuperCallWithPrivilege() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(SampleClassWithPrivilege.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getAuxiliaryTypes().size(), is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.value, is(BAR));
        instance.foo();
        assertThat(instance.value, is(FOO));
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

        public static void foo(@SuperMethod Method method, @This Object target) throws Exception {
            method.invoke(target);
        }
    }

    public static class SampleClassNoCache {

        public static void foo(@SuperMethod(cached = false) Method method, @This Object target) throws Exception {
            method.invoke(target);
        }
    }

    public static class SampleClassWithPrivilege {

        public static void foo(@SuperMethod(privileged = true) Method method, @This Object target) throws Exception {
            method.invoke(target);
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

    public static class NonVoidTarget {

        public static Object foo(@SuperMethod Method method, @This Object target) throws Exception {
            if (!Modifier.isPublic(method.getModifiers())) {
                throw new AssertionError();
            }
            return method.invoke(target);
        }
    }

    public static class IllegalAnnotation {

        public static String bar(@SuperMethod String value) throws Exception {
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class NoFallback {

        @RuntimeType
        public static Object foo(@SuperMethod(fallbackToDefault = false) Method method) throws Exception {
            return null;
        }
    }
}
