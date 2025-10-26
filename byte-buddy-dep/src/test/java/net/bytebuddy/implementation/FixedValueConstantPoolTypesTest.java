package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.CallTraceable;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class FixedValueConstantPoolTypesTest<T extends CallTraceable> {

    private static final String FOO = "foo", BAR = "bar";

    private static final String STRING_VALUE = "foo";

    private static final boolean BOOLEAN_VALUE = true;

    private static final byte BYTE_VALUE = 42;

    private static final short SHORT_VALUE = 42;

    private static final char CHAR_VALUE = '@';

    private static final int INT_VALUE = 42;

    private static final long LONG_VALUE = 42L;

    private static final float FLOAT_VALUE = 42f;

    private static final double DOUBLE_VALUE = 42d;

    private static final Void NULL_VALUE = null;

    private static final String STRING_DEFAULT_VALUE = "bar";

    private static final boolean BOOLEAN_DEFAULT_VALUE = false;

    private static final byte BYTE_DEFAULT_VALUE = 0;

    private static final short SHORT_DEFAULT_VALUE = 0;

    private static final char CHAR_DEFAULT_VALUE = 0;

    private static final int INT_DEFAULT_VALUE = 0;

    private static final long LONG_DEFAULT_VALUE = 0L;

    private static final float FLOAT_DEFAULT_VALUE = 0f;

    private static final double DOUBLE_DEFAULT_VALUE = 0d;

    private final Object fixedValue;

    private final Class<T> helperClass;

    public FixedValueConstantPoolTypesTest(Object fixedValue, Class<T> helperClass) {
        this.fixedValue = fixedValue;
        this.helperClass = helperClass;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {STRING_VALUE, StringTarget.class},
                {BOOLEAN_VALUE, BooleanTarget.class},
                {BYTE_VALUE, ByteTarget.class},
                {SHORT_VALUE, ShortTarget.class},
                {CHAR_VALUE, CharTarget.class},
                {INT_VALUE, IntTarget.class},
                {LONG_VALUE, LongTarget.class},
                {FLOAT_VALUE, FloatTarget.class},
                {DOUBLE_VALUE, DoubleTarget.class}
        });
    }

    @Test
    public void testConstantPool() throws Exception {
        DynamicType.Loaded<T> loaded = new ByteBuddy()
                .subclass(helperClass)
                .method(isDeclaredBy(helperClass))
                .intercept(FixedValue.value(fixedValue))
                .make()
                .load(helperClass.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(2));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        T instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(StringTarget.class)));
        assertThat(instance, instanceOf(helperClass));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO).invoke(instance), is(fixedValue));
        assertThat(loaded.getLoaded().getDeclaredMethod(BAR).invoke(instance), is(fixedValue));
        instance.assertZeroCalls();
    }

    @Test
    public void testStaticField() throws Exception {
        DynamicType.Loaded<T> loaded = new ByteBuddy()
                .subclass(helperClass)
                .method(isDeclaredBy(helperClass))
                .intercept(FixedValue.reference(fixedValue))
                .make()
                .load(helperClass.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(2));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(fixedValue == null ? 0 : 1));
        T instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(StringTarget.class)));
        assertThat(instance, instanceOf(helperClass));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO).invoke(instance), is(fixedValue));
        assertThat(loaded.getLoaded().getDeclaredMethod(BAR).invoke(instance), is(fixedValue));
        instance.assertZeroCalls();
    }

    @SuppressWarnings("unused")
    public static class StringTarget extends CallTraceable {

        public String foo() {
            register(FOO);
            return STRING_DEFAULT_VALUE;
        }

        public Object bar() {
            register(BAR);
            return STRING_DEFAULT_VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanTarget extends CallTraceable {

        public boolean foo() {
            register(FOO);
            return BOOLEAN_DEFAULT_VALUE;
        }

        public Boolean bar() {
            register(BAR);
            return BOOLEAN_DEFAULT_VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteTarget extends CallTraceable {

        public byte foo() {
            register(FOO);
            return BYTE_DEFAULT_VALUE;
        }

        public Byte bar() {
            register(BAR);
            return BYTE_DEFAULT_VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortTarget extends CallTraceable {

        public short foo() {
            register(FOO);
            return SHORT_DEFAULT_VALUE;
        }

        public Short bar() {
            register(BAR);
            return SHORT_DEFAULT_VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class CharTarget extends CallTraceable {

        public char foo() {
            register(FOO);
            return CHAR_DEFAULT_VALUE;
        }

        public Character bar() {
            register(BAR);
            return CHAR_DEFAULT_VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class IntTarget extends CallTraceable {

        public int foo() {
            register(FOO);
            return INT_DEFAULT_VALUE;
        }

        public Integer bar() {
            register(BAR);
            return INT_DEFAULT_VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class LongTarget extends CallTraceable {

        public long foo() {
            register(FOO);
            return LONG_DEFAULT_VALUE;
        }

        public Long bar() {
            register(BAR);
            return LONG_DEFAULT_VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatTarget extends CallTraceable {

        public float foo() {
            register(FOO);
            return FLOAT_DEFAULT_VALUE;
        }

        public Float bar() {
            register(BAR);
            return FLOAT_DEFAULT_VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleTarget extends CallTraceable {

        public double foo() {
            register(FOO);
            return DOUBLE_DEFAULT_VALUE;
        }

        public Double bar() {
            register(BAR);
            return DOUBLE_DEFAULT_VALUE;
        }
    }
}
