package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.Handle;
import net.bytebuddy.test.utility.AccessControllerRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaConstant;
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

public class MethodDelegationHandleTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public MethodRule accessControllerRule = new AccessControllerRule();

    @Test
    @JavaVersionRule.Enforce(value = 7, target = Foo.class)
    public void testHandle() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(SampleClass.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is(BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongTypeThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(IllegalAnnotation.class))
                .make();
    }

    public static class Foo {

        public static String bar() {
            return BAR;
        }

        public String foo() {
            return FOO;
        }
    }

    public static class SampleClass {

        public static String foo(@Handle(
                type = JavaConstant.MethodHandle.HandleType.INVOKE_STATIC,
                name = "bar",
                returnType = String.class,
                parameterTypes = {}) Object handle) throws Exception {
            Method method = Class.forName(JavaType.METHOD_HANDLE.getTypeStub().getName()).getMethod("invokeWithArguments", List.class);
            return (String) method.invoke(handle, Collections.emptyList());
        }
    }

    public static class IllegalAnnotation {

        public static String bar(@Handle(
                type = JavaConstant.MethodHandle.HandleType.INVOKE_STATIC,
                name = "bar",
                returnType = String.class,
                parameterTypes = {}) String value) throws Exception {
            return value;
        }
    }
}
