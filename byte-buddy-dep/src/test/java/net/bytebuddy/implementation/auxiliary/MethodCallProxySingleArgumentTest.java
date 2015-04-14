package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.test.utility.CallTraceable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Parameterized.class)
public class MethodCallProxySingleArgumentTest<T extends CallTraceable> extends AbstractMethodCallProxyTest {

    private static final String STRING_VALUE = "foo";

    private static final boolean BOOLEAN_VALUE = true;

    private static final byte BYTE_VALUE = 42;

    private static final short SHORT_VALUE = 42;

    private static final char CHAR_VALUE = '@';

    private static final int INT_VALUE = 42;

    private static final long LONG_VALUE = 42L;

    private static final float FLOAT_VALUE = 42f;

    private static final double DOUBLE_VALUE = 42d;

    private static final Object NULL_VALUE = null;

    private final Object value;

    private final Class<T> targetType;

    private final Class<?> valueType;

    public MethodCallProxySingleArgumentTest(Object value, Class<T> targetType, Class<?> valueType) {
        this.value = value;
        this.targetType = targetType;
        this.valueType = valueType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {STRING_VALUE, StringTarget.class, String.class},
                {BOOLEAN_VALUE, BooleanTarget.class, boolean.class},
                {BYTE_VALUE, ByteTarget.class, byte.class},
                {SHORT_VALUE, ShortTarget.class, short.class},
                {CHAR_VALUE, CharTarget.class, char.class},
                {INT_VALUE, IntTarget.class, int.class},
                {LONG_VALUE, LongTarget.class, long.class},
                {FLOAT_VALUE, FloatTarget.class, float.class},
                {DOUBLE_VALUE, DoubleTarget.class, double.class},
                {NULL_VALUE, NullTarget.class, Void.class}
        });
    }

    @Test
    public void testRunMethod() throws Exception {
        Class<?> auxiliaryType = proxyOnlyDeclaredMethodOf(targetType);
        Constructor<?> constructor = auxiliaryType.getDeclaredConstructor(targetType, valueType);
        constructor.setAccessible(true);
        T proxiedInstance = targetType.newInstance();
        ((Runnable) constructor.newInstance(proxiedInstance, value)).run();
        proxiedInstance.assertOnlyCall(FOO, value);
    }

    @Test
    public void testCallMethod() throws Exception {
        Class<?> auxiliaryType = proxyOnlyDeclaredMethodOf(targetType);
        Constructor<?> constructor = auxiliaryType.getDeclaredConstructor(targetType, valueType);
        constructor.setAccessible(true);
        T proxiedInstance = targetType.newInstance();
        assertThat(((Callable<?>) constructor.newInstance(proxiedInstance, value)).call(), is(value));
        proxiedInstance.assertOnlyCall(FOO, value);
    }

    @SuppressWarnings("unused")
    public static class StringTarget extends CallTraceable {

        public String foo(String s) {
            register(FOO, s);
            return s;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanTarget extends CallTraceable {

        public boolean foo(boolean b) {
            register(FOO, b);
            return b;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteTarget extends CallTraceable {

        public byte foo(byte b) {
            register(FOO, b);
            return b;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortTarget extends CallTraceable {

        public short foo(short s) {
            register(FOO, s);
            return s;
        }
    }

    @SuppressWarnings("unused")
    public static class CharTarget extends CallTraceable {

        public char foo(char c) {
            register(FOO, c);
            return c;
        }
    }

    @SuppressWarnings("unused")
    public static class IntTarget extends CallTraceable {

        public int foo(int i) {
            register(FOO, i);
            return i;
        }
    }

    @SuppressWarnings("unused")
    public static class LongTarget extends CallTraceable {

        public long foo(long l) {
            register(FOO, l);
            return l;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatTarget extends CallTraceable {

        public float foo(float f) {
            register(FOO, f);
            return f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleTarget extends CallTraceable {

        public double foo(double d) {
            register(FOO, d);
            return d;
        }
    }

    @SuppressWarnings("unused")
    public static class NullTarget extends CallTraceable {

        public void foo(Void v) {
            register(FOO, v);
        }
    }
}
