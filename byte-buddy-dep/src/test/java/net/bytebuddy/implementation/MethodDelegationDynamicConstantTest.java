package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationDynamicConstantTest {
    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @JavaVersionRule.Enforce(7)
    public void testDynamicConstantInvokedynamic() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.v7.MethodDelegationDynamicConstant");
        Class<?> type = new ByteBuddy()
                .subclass(bootstrap)
                .method(isDeclaredBy(bootstrap))
                .intercept(MethodDelegation.to(bootstrap))
                .make()
                .load(bootstrap.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getMethod(FOO).invoke(instance), instanceOf(bootstrap));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstant() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.v11.MethodDelegationDynamicConstant");
        Class<?> type = new ByteBuddy()
                .subclass(bootstrap)
                .method(isDeclaredBy(bootstrap))
                .intercept(MethodDelegation.to(bootstrap))
                .make()
                .load(bootstrap.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getMethod(FOO).invoke(instance), instanceOf(bootstrap));
    }
}
