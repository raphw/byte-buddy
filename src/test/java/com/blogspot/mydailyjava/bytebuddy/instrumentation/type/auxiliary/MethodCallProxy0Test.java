package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MethodCallProxy0Test {

    private static final ClassFormatVersion CLASS_VERSION = new ClassFormatVersion(Opcodes.V1_6);

    private static final String RUN_METHOD = "run";
    private static final String CALL_METHOD = "call";
    private static final String FIELD_NAME_PREFIX = "arg";
    private static final String FOO = "foo";

    private static interface InvocationCountable {

        int getNumberOfInvocations();
    }

    @SuppressWarnings("unused")
    public static class Bar implements InvocationCountable {

        private int invocations;

        public void foo() {
            invocations++;
        }

        @Override
        public int getNumberOfInvocations() {
            return invocations;
        }
    }

    @SuppressWarnings("unused")
    public static class Qux implements InvocationCountable {

        private int invocations;

        public void foo(String a, int b, Object[] c, long d) {
            invocations++;
        }

        @Override
        public int getNumberOfInvocations() {
            return invocations;
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Bar.class, new Class<?>[0], new Object[0]},
//                {Qux.class, new Class<?>[]{String.class, int.class, Object[].class, long.class}, new Object[] {FOO, 21, new Object[0], 42L}},
        });
    }

    private Class<? extends InvocationCountable> proxiedType;
    private Class<?>[] proxiedMethodParameters;
    private Object[] methodArgument;

    public MethodCallProxy0Test(Class<? extends InvocationCountable> proxiedType,
                                Class<?>[] proxiedMethodParameters,
                                Object[] methodArgument) {
        this.proxiedType = proxiedType;
        this.proxiedMethodParameters = proxiedMethodParameters;
        this.methodArgument = methodArgument;
    }

    @Test
    public void testMethodProxy() throws Exception {
        MethodDescription proxiedMethod = new MethodDescription.ForMethod
                (proxiedType.getDeclaredMethod(FOO, proxiedMethodParameters));
        MethodCallProxy0 methodCallProxy = new MethodCallProxy0(proxiedMethod);
        DynamicType<?> dynamicType = methodCallProxy.make(proxyName(proxiedType), null, null); // TODO
        ClassLoader proxyClassLoader = new ByteArrayClassLoader(getClass().getClassLoader(),
                Collections.singletonMap(dynamicType.getName(), dynamicType.getBytes()));
        Class<?> proxyType = Class.forName(proxyName(proxiedType), false, proxyClassLoader);
        Constructor<?> proxyConstructor = assertProxyType(proxyType, proxiedMethod);
        InvocationCountable invocationCountable = proxiedType.newInstance();
        Object[] methodArgument = new Object[this.methodArgument.length + (proxiedMethod.isStatic() ? 0 : 1)];
        int index = 0;
        if (!proxiedMethod.isStatic()) {
            methodArgument[index++] = invocationCountable;
        }
        for (Object argument : this.methodArgument) {
            methodArgument[index++] = argument;
        }
        Object methodCallProxyInstance = proxyConstructor.newInstance(methodArgument);
        assertProxyInvocations(invocationCountable, methodCallProxyInstance);
    }

    private static Constructor<?> assertProxyType(Class<?> proxyType, MethodDescription methodDescription) throws Exception {
        Class<?> proxiedType = Class.forName(methodDescription.getDeclaringType().getName());
        assertThat(proxyType.getName(), is(proxyName(proxiedType)));
        assertEquals(Object.class, proxyType.getSuperclass());
        assertThat(proxyType.getInterfaces().length, is(2));
        assertThat(proxyType.getDeclaredMethods().length, is(2));
        assertThat(proxyType.getDeclaredMethod(RUN_METHOD), notNullValue());
        assertThat(proxyType.getDeclaredMethod(CALL_METHOD), notNullValue());
        assertThat(proxyType.getDeclaredConstructors().length, is(1));
        int expectedMethodFields = (methodDescription.isStatic() ? 0 : 1) + methodDescription.getParameterTypes().size();
        assertThat(proxyType.getDeclaredFields().length, is(expectedMethodFields));
        Class<?>[] expectedConstructorArguments = new Class<?>[expectedMethodFields];
        int index = 0;
        if (!methodDescription.isStatic()) {
            assertEquals(proxiedType, proxyType.getDeclaredField(FIELD_NAME_PREFIX + index).getType());
            expectedConstructorArguments[index] = proxiedType;
            index++;
        }
        for (TypeDescription parameterTypeDescription : methodDescription.getParameterTypes()) {
            Class<?> parameterType = Class.forName(parameterTypeDescription.getName());
            assertEquals(parameterType, proxyType.getDeclaredField(FIELD_NAME_PREFIX + index).getType());
            expectedConstructorArguments[index] = parameterType;
            index++;
        }
        assertThat(proxyType.getDeclaredConstructor(expectedConstructorArguments), notNullValue());
        return proxyType.getDeclaredConstructor(expectedConstructorArguments);
    }

    private static void assertProxyInvocations(InvocationCountable invocationCountable, Object proxy) throws Exception {
        assertThat(invocationCountable.getNumberOfInvocations(), is(0));
        assertThat(proxy instanceof Runnable, is(true));
        ((Runnable) proxy).run();
        assertThat(invocationCountable.getNumberOfInvocations(), is(1));
        assertThat(proxy instanceof Callable, is(true));
        ((Callable<?>) proxy).call();
        assertThat(invocationCountable.getNumberOfInvocations(), is(2));
    }

    private static String proxyName(Class<?> type) {
        return type.getName() + "$FooProxy";
    }
}
