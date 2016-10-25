package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.CallTraceable;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class MethodDelegationConstructionTest<T extends CallTraceable> {

    private static final String FOO = "foo", BAR = "bar";

    private static final byte BYTE_MULTIPLICATOR = 3;

    private static final short SHORT_MULTIPLICATOR = 3;

    private static final char CHAR_MULTIPLICATOR = 3;

    private static final int INT_MULTIPLICATOR = 3;

    private static final long LONG_MULTIPLICATOR = 3L;

    private static final float FLOAT_MULTIPLICATOR = 3f;

    private static final double DOUBLE_MULTIPLICATOR = 3d;

    private static final boolean DEFAULT_BOOLEAN = false;

    private static final byte DEFAULT_BYTE = 1;

    private static final short DEFAULT_SHORT = 1;

    private static final char DEFAULT_CHAR = 1;

    private static final int DEFAULT_INT = 1;

    private static final long DEFAULT_LONG = 1L;

    private static final float DEFAULT_FLOAT = 1f;

    private static final double DEFAULT_DOUBLE = 1d;

    private final Class<T> sourceType;

    private final Class<?> targetType;

    private final Class<?>[] parameterTypes;

    private final Object[] arguments;

    private final Matcher<?> matcher;

    public MethodDelegationConstructionTest(Class<T> sourceType,
                                            Class<?> targetType,
                                            Class<?>[] parameterTypes,
                                            Object[] arguments,
                                            Matcher<?> matcher) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.parameterTypes = parameterTypes;
        this.arguments = arguments;
        this.matcher = matcher;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BooleanSource.class, BooleanTarget.class, new Class<?>[]{boolean.class}, new Object[]{DEFAULT_BOOLEAN}, is(!DEFAULT_BOOLEAN)},
                {ByteSource.class, ByteTarget.class, new Class<?>[]{byte.class}, new Object[]{DEFAULT_BYTE}, is((byte) (DEFAULT_BYTE * BYTE_MULTIPLICATOR))},
                {ShortSource.class, ShortTarget.class, new Class<?>[]{short.class}, new Object[]{DEFAULT_SHORT}, is((short) (DEFAULT_SHORT * SHORT_MULTIPLICATOR))},
                {CharSource.class, CharTarget.class, new Class<?>[]{char.class}, new Object[]{DEFAULT_CHAR}, is((char) (DEFAULT_CHAR * CHAR_MULTIPLICATOR))},
                {IntSource.class, IntTarget.class, new Class<?>[]{int.class}, new Object[]{DEFAULT_INT}, is(DEFAULT_INT * INT_MULTIPLICATOR)},
                {LongSource.class, LongTarget.class, new Class<?>[]{long.class}, new Object[]{DEFAULT_LONG}, is(DEFAULT_LONG * LONG_MULTIPLICATOR)},
                {FloatSource.class, FloatTarget.class, new Class<?>[]{float.class}, new Object[]{DEFAULT_FLOAT}, is(DEFAULT_FLOAT * FLOAT_MULTIPLICATOR)},
                {DoubleSource.class, DoubleTarget.class, new Class<?>[]{double.class}, new Object[]{DEFAULT_DOUBLE}, is(DEFAULT_DOUBLE * DOUBLE_MULTIPLICATOR)},
                {VoidSource.class, VoidTarget.class, new Class<?>[0], new Object[0], nullValue()},
                {StringSource.class, StringTarget.class, new Class<?>[]{String.class}, new Object[]{FOO}, is(FOO + BAR)},
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConstruction() throws Exception {
        DynamicType.Loaded<T> loaded = new ByteBuddy()
                .subclass(sourceType)
                .method(isDeclaredBy(sourceType))
                .intercept(MethodDelegation.toConstructor(targetType))
                .make()
                .load(sourceType.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        T instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(sourceType)));
        assertThat(instance, instanceOf(sourceType));
        Object value = loaded.getLoaded().getDeclaredMethod(FOO, parameterTypes).invoke(instance, arguments);
        assertThat(value, instanceOf(targetType));
        Field field = targetType.getDeclaredField("value");
        field.setAccessible(true);
        assertThat(field.get(value), (Matcher) matcher);
        instance.assertZeroCalls();
    }

    public static class BooleanSource extends CallTraceable {

        @SuppressWarnings("unused")
        public BooleanTarget foo(boolean b) {
            register(FOO);
            return null;
        }
    }

    public static class BooleanTarget {

        @SuppressWarnings("unused")
        private final boolean value;

        public BooleanTarget(boolean value) {
            this.value = !value;
        }
    }

    public static class ByteSource extends CallTraceable {

        @SuppressWarnings("unused")
        public ByteTarget foo(byte b) {
            register(FOO);
            return null;
        }
    }

    public static class ByteTarget {

        @SuppressWarnings("unused")
        private final byte value;

        public ByteTarget(byte b) {
            value = bar(b);
        }

        private static byte bar(byte b) {
            return (byte) (b * BYTE_MULTIPLICATOR);
        }
    }

    public static class ShortSource extends CallTraceable {

        @SuppressWarnings("unused")
        public ShortTarget foo(short s) {
            register(FOO);
            return null;
        }
    }

    public static class ShortTarget {

        @SuppressWarnings("unused")
        private final short value;

        public ShortTarget(short s) {
            this.value = bar(s);
        }

        private static short bar(short s) {
            return (short) (s * SHORT_MULTIPLICATOR);
        }
    }

    public static class CharSource extends CallTraceable {

        @SuppressWarnings("unused")
        public CharTarget foo(char s) {
            register(FOO);
            return null;
        }
    }

    public static class CharTarget {

        @SuppressWarnings("unused")
        private final char value;

        public CharTarget(char c) {
            this.value = bar(c);
        }

        private static char bar(char c) {
            return (char) (c * CHAR_MULTIPLICATOR);
        }
    }

    public static class IntSource extends CallTraceable {

        @SuppressWarnings("unused")
        public IntTarget foo(int i) {
            register(FOO);
            return null;
        }
    }

    public static class IntTarget {

        @SuppressWarnings("unused")
        private final int value;

        public IntTarget(int i) {
            this.value = bar(i);
        }

        private static int bar(int i) {
            return i * INT_MULTIPLICATOR;
        }
    }

    public static class LongSource extends CallTraceable {

        @SuppressWarnings("unused")
        public LongTarget foo(long l) {
            register(FOO);
            return null;
        }
    }

    public static class LongTarget {

        @SuppressWarnings("unused")
        private final long value;

        public LongTarget(long l) {
            this.value = bar(l);
        }

        private static long bar(long l) {
            return l * LONG_MULTIPLICATOR;
        }
    }

    public static class FloatSource extends CallTraceable {

        @SuppressWarnings("unused")
        public FloatTarget foo(float f) {
            register(FOO);
            return null;
        }
    }

    public static class FloatTarget {

        @SuppressWarnings("unused")
        private final float value;

        public FloatTarget(float f) {
            this.value = bar(f);
        }

        private static float bar(float f) {
            return f * FLOAT_MULTIPLICATOR;
        }
    }

    public static class DoubleSource extends CallTraceable {

        @SuppressWarnings("unused")
        public DoubleTarget foo(double d) {
            register(FOO);
            return null;
        }
    }

    public static class DoubleTarget {

        @SuppressWarnings("unused")

        private final double value;

        public DoubleTarget(double d) {
            this.value = bar(d);
        }

        public static double bar(double d) {
            return d * DOUBLE_MULTIPLICATOR;
        }
    }

    public static class VoidSource extends CallTraceable {

        public VoidTarget foo() {
            register(FOO);
            return null;
        }
    }

    public static class VoidTarget {

        @SuppressWarnings("unused")
        private final Void value = null;
    }

    public static class StringSource extends CallTraceable {

        public StringTarget foo(String s) {
            register(FOO);
            return null;
        }
    }

    public static class StringTarget {

        @SuppressWarnings("unused")
        private final String value;

        public StringTarget(String s) {
            this.value = bar(s);
        }

        public static String bar(String s) {
            return s + BAR;
        }
    }
}
