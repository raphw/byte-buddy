package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCallHandle;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationSuperCallHandleTest {

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.v8.SingleDefaultMethodInterface";

    private static final String CONFLICTING_INTERFACE = "net.bytebuddy.test.precompiled.v8.SingleDefaultMethodConflictingInterface";

    private static final String FOO = "foo", BAR = "bar";

    private static final int QUX = 42, BAZ = 84;

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodHandleCall() throws Exception {
        DynamicType.Loaded<Bar> loaded = new ByteBuddy()
                .subclass(Bar.class)
                .method(isDeclaredBy(Bar.class))
                .intercept(MethodDelegation.to(ReferenceTarget.class))
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Bar instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.bar(), is(FOO));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodHandleCallWithPrimitiveArgument() throws Exception {
        DynamicType.Loaded<Baz> loaded = new ByteBuddy()
                .subclass(Baz.class)
                .method(isDeclaredBy(Baz.class))
                .intercept(MethodDelegation.to(PrimitiveTarget.class))
                .make()
                .load(Baz.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Baz instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.baz(BAZ), is((Object) (QUX + BAZ)));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodHandleCallWithReferenceArgument() throws Exception {
        DynamicType.Loaded<Foobar> loaded = new ByteBuddy()
                .subclass(Foobar.class)
                .method(isDeclaredBy(Foobar.class))
                .intercept(MethodDelegation.to(ReferenceTarget.class))
                .make()
                .load(Foobar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Foobar instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.baz(FOO), is((Object) (FOO + BAR)));
    }

    @Test
    @JavaVersionRule.Enforce(7)
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
    @JavaVersionRule.Enforce(7)
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
    @JavaVersionRule.Enforce(7)
    public void testAbstractMethodNonBindable() throws Exception {
        new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(MethodDelegation.to(ReferenceTarget.class))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(7)
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

    public static class Bar {

        public String bar() {
            return FOO;
        }
    }

    public static class ReferenceTarget {

        public static String bar(@SuperCallHandle Object handle) throws Exception {
            Method method = Class.forName(JavaType.METHOD_HANDLE.getTypeStub().getName()).getMethod("invokeWithArguments", List.class);
            return (String) method.invoke(handle, Collections.emptyList());
        }
    }

    public abstract static class Qux {

        public abstract String bar();
    }

    public static class Baz {

        public int value = QUX;

        public int baz(int value) {
            return value + QUX;
        }
    }

    public static class Foobar {

        public String value = FOO;

        public String baz(String value) {
            return value + BAR;
        }
    }

    public static class PrimitiveTarget {

        public static int bar(@SuperCallHandle Object handle) throws Exception {
            Method method = Class.forName(JavaType.METHOD_HANDLE.getTypeStub().getName()).getMethod("invokeWithArguments", List.class);
            return (Integer) method.invoke(handle, Collections.emptyList());
        }
    }

    public static class VoidTest extends CallTraceable {

        public void foo() {
            register(FOO);
        }
    }

    public static class NonVoidTarget {

        public static Object foo(@SuperCallHandle Object handle) throws Exception {
            Method method = Class.forName(JavaType.METHOD_HANDLE.getTypeStub().getName()).getMethod("invokeWithArguments", List.class);
            return method.invoke(handle, Collections.emptyList());
        }
    }

    public static class RuntimeTypeTest {

        public String foo() {
            return FOO;
        }
    }

    public static class RuntimeTypeTarget {

        @RuntimeType
        public static Object foo(@SuperCallHandle Object handle) throws Exception {
            Method method = Class.forName(JavaType.METHOD_HANDLE.getTypeStub().getName()).getMethod("invokeWithArguments", List.class);
            return method.invoke(handle, Collections.emptyList());
        }
    }

    public static class IllegalAnnotation {

        public static String bar(@SuperCallHandle String value) throws Exception {
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class NoFallback {

        @RuntimeType
        public static Object foo(@SuperCallHandle(fallbackToDefault = false) Object handle) throws Exception {
            return null;
        }
    }
}
