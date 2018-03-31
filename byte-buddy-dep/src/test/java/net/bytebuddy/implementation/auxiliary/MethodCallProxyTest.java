package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.test.utility.CallTraceable;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodCallProxyTest extends AbstractMethodCallProxyTest {

    private static final long LONG_VALUE = 42L;

    private static final String String_VALUE = "BAR";

    private static final int INT_VALUE = 21;

    private static final boolean BOOLEAN_VALUE = true;

    @Test
    public void testNoParameterMethod() throws Exception {
        Class<?> auxiliaryType = proxyOnlyDeclaredMethodOf(NoParameterMethod.class);
        Constructor<?> constructor = auxiliaryType.getDeclaredConstructor(NoParameterMethod.class);
        constructor.setAccessible(true);
        NoParameterMethod runnableProxied = new NoParameterMethod();
        ((Runnable) constructor.newInstance(runnableProxied)).run();
        runnableProxied.assertOnlyCall(FOO, runnableProxied);
        NoParameterMethod callableProxied = new NoParameterMethod();
        assertThat(((Callable<?>) constructor.newInstance(callableProxied)).call(), nullValue());
        callableProxied.assertOnlyCall(FOO, callableProxied);
    }

    @Test
    public void testStaticMethod() throws Exception {
        Class<?> auxiliaryType = proxyOnlyDeclaredMethodOf(StaticMethod.class);
        Constructor<?> constructor = auxiliaryType.getDeclaredConstructor();
        constructor.setAccessible(true);
        ((Runnable) constructor.newInstance()).run();
        StaticMethod.CALL_TRACEABLE.assertOnlyCall(FOO);
        StaticMethod.CALL_TRACEABLE.reset();
        assertThat(((Callable<?>) constructor.newInstance()).call(), nullValue());
        StaticMethod.CALL_TRACEABLE.assertOnlyCall(FOO);
        StaticMethod.CALL_TRACEABLE.reset();
    }

    @Test
    public void testMultipleParameterMethod() throws Exception {
        Class<?> auxiliaryType = proxyOnlyDeclaredMethodOf(MultipleParameterMethod.class);
        Constructor<?> constructor = auxiliaryType.getDeclaredConstructor(MultipleParameterMethod.class,
                long.class,
                String.class,
                int.class,
                boolean.class);
        constructor.setAccessible(true);
        MultipleParameterMethod runnableProxied = new MultipleParameterMethod();
        Object[] runnableArguments = new Object[]{runnableProxied, LONG_VALUE, String_VALUE, INT_VALUE, BOOLEAN_VALUE};
        ((Runnable) constructor.newInstance(runnableArguments)).run();
        runnableProxied.assertOnlyCall(FOO, runnableArguments);
        MultipleParameterMethod callableProxied = new MultipleParameterMethod();
        Object[] callableArguments = new Object[]{callableProxied, LONG_VALUE, String_VALUE, INT_VALUE, BOOLEAN_VALUE};
        assertThat(((Callable<?>) constructor.newInstance(callableArguments)).call(), nullValue());
        callableProxied.assertOnlyCall(FOO, callableArguments);
    }

    @Test
    public void testNonGenericParameter() throws Exception {
        Class<?> auxiliaryType = proxyOnlyDeclaredMethodOf(GenericType.class);
        assertThat(auxiliaryType.getTypeParameters().length, is(0));
        assertThat(auxiliaryType.getDeclaredMethod("call").getGenericReturnType(), is((Type) Object.class));
        assertThat(auxiliaryType.getDeclaredFields()[1].getGenericType(), is((Type) Object.class));
        assertThat(auxiliaryType.getDeclaredFields()[2].getGenericType(), is((Type) Number.class));
    }

    @SuppressWarnings("unused")
    public static class NoParameterMethod extends CallTraceable {

        public void foo() {
            register(FOO, this);
        }
    }

    @SuppressWarnings("unused")
    public static class StaticMethod extends CallTraceable {

        public static final CallTraceable CALL_TRACEABLE = new CallTraceable();

        public static void foo() {
            CALL_TRACEABLE.register(FOO);
        }
    }

    @SuppressWarnings("unused")
    public static class MultipleParameterMethod extends CallTraceable {

        public void foo(long l, String s, int i, boolean b) {
            register(FOO, this, l, s, i, b);
        }
    }

    @SuppressWarnings("unused")
    public static class GenericType<T, S extends Number> {

        T foo(T t, S s) {
            return t;
        }
    }
}
