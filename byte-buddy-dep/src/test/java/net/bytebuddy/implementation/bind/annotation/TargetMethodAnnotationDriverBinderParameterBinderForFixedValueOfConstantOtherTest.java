package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TargetMethodAnnotationDriverBinderParameterBinderForFixedValueOfConstantOtherTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testTypeDescription() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(Foo.class)
                .method(named(FOO))
                .intercept(MethodDelegation.to(Foo.class)
                        .appendParameterBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue.OfConstant.of(Bar.class, TypeDescription.OBJECT)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .foo(), is((Object) Object.class));
    }

    @Test
    public void testNull() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(Foo.class)
                .method(named(FOO))
                .intercept(MethodDelegation.to(Foo.class)
                        .appendParameterBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue.OfConstant.of(Bar.class, null)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .foo(), nullValue(Object.class));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodHandleLoaded() throws Exception {
        Method publicLookup = Class.forName("java.lang.invoke.MethodHandles").getDeclaredMethod("publicLookup");
        Object lookup = publicLookup.invoke(null);
        Method unreflected = Class.forName("java.lang.invoke.MethodHandles$Lookup").getDeclaredMethod("unreflect", Method.class);
        Object methodHandleLoaded = unreflected.invoke(lookup, Foo.class.getDeclaredMethod(FOO));
        assertThat(JavaConstant.MethodHandle.ofLoaded(new ByteBuddy()
                .subclass(Foo.class)
                .method(named(FOO))
                .intercept(MethodDelegation.to(Foo.class)
                        .appendParameterBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue.OfConstant.of(Bar.class, methodHandleLoaded)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .foo()), is(JavaConstant.MethodHandle.ofLoaded(methodHandleLoaded)));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodHandle() throws Exception {
        Method publicLookup = Class.forName("java.lang.invoke.MethodHandles").getDeclaredMethod("publicLookup");
        Object lookup = publicLookup.invoke(null);
        Method unreflected = Class.forName("java.lang.invoke.MethodHandles$Lookup").getDeclaredMethod("unreflect", Method.class);
        Object methodHandleLoaded = unreflected.invoke(lookup, Foo.class.getDeclaredMethod(FOO));
        assertThat(JavaConstant.MethodHandle.ofLoaded(new ByteBuddy()
                .subclass(Foo.class)
                .method(named(FOO))
                .intercept(MethodDelegation.to(Foo.class)
                        .appendParameterBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue.OfConstant.of(Bar.class, JavaConstant.MethodHandle.ofLoaded(methodHandleLoaded))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .foo()), is(JavaConstant.MethodHandle.ofLoaded(methodHandleLoaded)));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodTypeLoaded() throws Exception {
        Object loadedMethodType = JavaType.METHOD_TYPE.load().getDeclaredMethod("methodType", Class.class, Class[].class)
                .invoke(null, void.class, new Class<?>[]{Object.class});
        assertThat(JavaConstant.MethodType.ofLoaded(new ByteBuddy()
                .subclass(Foo.class)
                .method(named(FOO))
                .intercept(MethodDelegation.to(Foo.class)
                        .appendParameterBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue.OfConstant.of(Bar.class, loadedMethodType)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .foo()), is(JavaConstant.MethodType.ofLoaded(loadedMethodType)));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodType() throws Exception {
        Object loadedMethodType = JavaType.METHOD_TYPE.load().getDeclaredMethod("methodType", Class.class, Class[].class)
                .invoke(null, void.class, new Class<?>[]{Object.class});
        assertThat(JavaConstant.MethodType.ofLoaded(new ByteBuddy()
                .subclass(Foo.class)
                .method(named(FOO))
                .intercept(MethodDelegation.to(Foo.class)
                        .appendParameterBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue.OfConstant.of(Bar.class, JavaConstant.MethodType.ofLoaded(loadedMethodType))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .foo()), is(JavaConstant.MethodType.ofLoaded(loadedMethodType)));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalArgument() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(named(FOO))
                .intercept(MethodDelegation.to(Foo.class)
                        .appendParameterBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue.OfConstant.of(Bar.class, new Object())))
                .make();
    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Class<?>> iterator = Arrays.<Class<?>>asList(Object.class, String.class, int.class, float.class).iterator();
        ObjectPropertyAssertion.of(TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue.OfConstant.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return iterator.next();
            }
        }).apply();
    }

    public static class Foo {

        public static Object intercept(@Bar Object value) {
            return value;
        }

        public Object foo() {
            throw new AssertionError();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Bar {
        /* empty */
    }
}
