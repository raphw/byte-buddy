package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.FieldSetterHandle;
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

public class MethodDelegationFieldSetterHandleTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @JavaVersionRule.Enforce(value = 7, target = SimpleField.class)
    public void testLegalFieldAccess() throws Exception {
        DynamicType.Loaded<SimpleField> loaded = new ByteBuddy()
                .subclass(SimpleField.class)
                .method(isDeclaredBy(SimpleField.class))
                .intercept(MethodDelegation.to(SimpleInterceptor.class))
                .make()
                .load(SimpleField.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        SimpleField instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo();
        assertThat(instance.foo, is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = SimpleStaticField.class)
    public void testLegalFieldAccessStatic() throws Exception {
        DynamicType.Loaded<SimpleStaticField> loaded = new ByteBuddy()
                .subclass(SimpleStaticField.class)
                .method(isDeclaredBy(SimpleStaticField.class))
                .intercept(MethodDelegation.to(SimpleInterceptor.class))
                .make()
                .load(SimpleStaticField.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        SimpleStaticField instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo();
        assertThat(SimpleStaticField.foo, is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = SimpleField.class)
    public void testLegalFieldAccessExplicit() throws Exception {
        DynamicType.Loaded<SimpleField> loaded = new ByteBuddy()
                .subclass(SimpleField.class)
                .method(isDeclaredBy(SimpleField.class))
                .intercept(MethodDelegation.to(ExplicitInterceptor.class))
                .make()
                .load(SimpleField.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        SimpleField instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo();
        assertThat(instance.foo, is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = SimpleFieldAccessor.class)
    public void testLegalFieldAccessBean() throws Exception {
        DynamicType.Loaded<SimpleFieldAccessor> loaded = new ByteBuddy()
                .subclass(SimpleFieldAccessor.class)
                .method(isDeclaredBy(SimpleFieldAccessor.class))
                .intercept(MethodDelegation.to(SimpleAccessorInterceptor.class))
                .make()
                .load(SimpleFieldAccessor.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        SimpleFieldAccessor instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.setFoo(FOO);
        assertThat(instance.foo, is((Object) FOO));
    }

    public static class SimpleField {

        public Object foo;

        public void foo() {
            /* empty */
        }
    }

    public static class SimpleStaticField {

        public static Object foo;

        public void foo() {
            /* empty */
        }
    }

    public static class SimpleFieldAccessor {

        public Object foo;

        public Object getFoo() {
            return foo;
        }

        public void setFoo(Object foo) {
            this.foo = foo;
        }
    }

    public static class SimpleInterceptor {

        public static void intercept(@FieldSetterHandle(FOO) Object handle) throws Throwable {
            Method method = Class.forName(JavaType.METHOD_HANDLE.getTypeStub().getName()).getMethod("invokeWithArguments", List.class);
            method.invoke(handle, Collections.singletonList(FOO));
        }
    }

    public static class ExplicitInterceptor {

        public static void intercept(@FieldSetterHandle(value = FOO, declaringType = SimpleField.class) Object handle) throws Throwable {
            Method method = Class.forName(JavaType.METHOD_HANDLE.getTypeStub().getName()).getMethod("invokeWithArguments", List.class);
            method.invoke(handle, Collections.singletonList(FOO));
        }
    }

    public static class SimpleAccessorInterceptor {

        public static Object intercept(@FieldSetterHandle Object handle) throws Throwable {
            Method method = Class.forName(JavaType.METHOD_HANDLE.getTypeStub().getName()).getMethod("invokeWithArguments", List.class);
            method.invoke(handle, Collections.singletonList(FOO));
            return null;
        }
    }
}
