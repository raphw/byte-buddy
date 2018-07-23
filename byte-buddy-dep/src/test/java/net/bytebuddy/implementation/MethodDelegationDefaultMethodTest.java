package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.DefaultMethod;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationDefaultMethodTest {

    private static final String FOO = "foo", QUX = "qux";

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    private static final String CONFLICTING_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodConflictingInterface";

    private static final String PREFERRING_INTERCEPTOR = "net.bytebuddy.test.precompiled.SingleDefaultMethodPreferringInterceptor";

    private static final String CONFLICTING_PREFERRING_INTERCEPTOR = "net.bytebuddy.test.precompiled.SingleDefaultMethodConflictingPreferringInterceptor";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @JavaVersionRule.Enforce(8)
    public void testCallableDefaultCall() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD))
                .intercept(MethodDelegation.to(SampleClass.class))
                .make()
                .load(Class.forName(SINGLE_DEFAULT_METHOD).getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testCallableDefaultCallNoCache() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD))
                .intercept(MethodDelegation.to(SampleClassNoCache.class))
                .make()
                .load(Class.forName(SINGLE_DEFAULT_METHOD).getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testCallableDefaultCallPrivileged() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD))
                .intercept(MethodDelegation.to(SampleClassPrivileged.class))
                .make()
                .load(Class.forName(SINGLE_DEFAULT_METHOD).getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getAuxiliaryTypes().size(), is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(8)
    public void testImplicitAmbiguousDefaultCallIsBoundToFirst() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE))
                .defineMethod(FOO, Object.class, Visibility.PUBLIC)
                .intercept(MethodDelegation.to(SampleClass.class))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testExplicitDefaultCall() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE))
                .intercept(MethodDelegation.to(Class.forName(PREFERRING_INTERCEPTOR)))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testExplicitDefaultCallToOtherInterface() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE))
                .intercept(MethodDelegation.to(Class.forName(CONFLICTING_PREFERRING_INTERCEPTOR)))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) QUX));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testIllegalDefaultCallThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD))
                .intercept(MethodDelegation.to(IllegalAnnotation.class))
                .make();
    }

    public static class SampleClass {

        public static String bar(@DefaultMethod Method method, @This Object target) throws Exception {
            if (!Modifier.isPublic(method.getModifiers())) {
                throw new AssertionError();
            }
            return (String) method.invoke(target);
        }
    }

    public static class SampleClassNoCache {

        public static String bar(@DefaultMethod(cached = false) Method method, @This Object target) throws Exception {
            if (!Modifier.isPublic(method.getModifiers())) {
                throw new AssertionError();
            }
            return (String) method.invoke(target);
        }
    }

    public static class SampleClassPrivileged {

        public static String bar(@DefaultMethod(privileged = true) Method method, @This Object target) throws Exception {
            if (!Modifier.isPublic(method.getModifiers())) {
                throw new AssertionError();
            }
            return (String) method.invoke(target);
        }
    }

    public static class IllegalAnnotation {

        public static String bar(@DefaultMethod String value) throws Exception {
            return value;
        }
    }
}
