package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceSkipOnNonDefaultValueTest {

    private static final String FOO = "foo";
    
    private static final int BAR = 42;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BooleanAdvice.class, false},
                {ByteAdvice.class, (byte) 0},
                {ShortAdvice.class, (short) 0},
                {CharacterAdvice.class, (char) 0},
                {IntegerAdvice.class, 0},
                {LongAdvice.class, 0L},
                {FloatAdvice.class, 0f},
                {DoubleAdvice.class, 0d},
                {ReferenceAdvice.class, null},
                {VoidAdvice.class, null},
                {BooleanDelegateAdvice.class, false},
                {ByteDelegateAdvice.class, (byte) 0},
                {ShortDelegateAdvice.class, (short) 0},
                {CharacterDelegateAdvice.class, (char) 0},
                {IntegerDelegateAdvice.class, 0},
                {LongDelegateAdvice.class, 0L},
                {FloatDelegateAdvice.class, 0f},
                {DoubleDelegateAdvice.class, 0d},
                {ReferenceDelegateAdvice.class, null},
                {VoidDelegateAdvice.class, null},
                {BooleanWithOutExitAdvice.class, false},
                {ByteWithOutExitAdvice.class, (byte) 0},
                {ShortWithOutExitAdvice.class, (short) 0},
                {CharacterWithOutExitAdvice.class, (char) 0},
                {IntegerWithOutExitAdvice.class, 0},
                {LongWithOutExitAdvice.class, 0L},
                {FloatWithOutExitAdvice.class, 0f},
                {DoubleWithOutExitAdvice.class, 0d},
                {ReferenceWithOutExitAdvice.class, null},
                {VoidWithOutExitAdvice.class, null},
                {BooleanDelegateWithOutExitAdvice.class, false},
                {ByteDelegateWithOutExitAdvice.class, (byte) 0},
                {ShortDelegateWithOutExitAdvice.class, (short) 0},
                {CharacterDelegateWithOutExitAdvice.class, (char) 0},
                {IntegerDelegateWithOutExitAdvice.class, 0},
                {LongDelegateWithOutExitAdvice.class, 0L},
                {FloatDelegateWithOutExitAdvice.class, 0f},
                {DoubleDelegateWithOutExitAdvice.class, 0d},
                {ReferenceDelegateWithOutExitAdvice.class, null},
                {VoidDelegateWithOutExitAdvice.class, null},
                {BooleanNoSkipAdvice.class, true},
                {ByteNoSkipAdvice.class, (byte) BAR},
                {ShortNoSkipAdvice.class, (short) BAR},
                {CharacterNoSkipAdvice.class, (char) BAR},
                {IntegerNoSkipAdvice.class, BAR},
                {LongNoSkipAdvice.class, (long) BAR},
                {FloatNoSkipAdvice.class, (float) BAR},
                {DoubleNoSkipAdvice.class, (double) BAR},
                {ReferenceNoSkipAdvice.class, FOO},
                {VoidNoSkipAdvice.class, null},
                {BooleanDelegateNoSkipAdvice.class, true},
                {ByteDelegateNoSkipAdvice.class, (byte) BAR},
                {ShortDelegateNoSkipAdvice.class, (short) BAR},
                {CharacterDelegateNoSkipAdvice.class, (char) BAR},
                {IntegerDelegateNoSkipAdvice.class, BAR},
                {LongDelegateNoSkipAdvice.class, (long) BAR},
                {FloatDelegateNoSkipAdvice.class, (float) BAR},
                {DoubleDelegateNoSkipAdvice.class, (double) BAR},
                {ReferenceDelegateNoSkipAdvice.class, FOO},
                {VoidDelegateNoSkipAdvice.class, null},
                {BooleanWithOutExitNoSkipAdvice.class, true},
                {ByteWithOutExitNoSkipAdvice.class, (byte) BAR},
                {ShortWithOutExitNoSkipAdvice.class, (short) BAR},
                {CharacterWithOutExitNoSkipAdvice.class, (char) BAR},
                {IntegerWithOutExitNoSkipAdvice.class, BAR},
                {LongWithOutExitNoSkipAdvice.class, (long) BAR},
                {FloatWithOutExitNoSkipAdvice.class, (float) BAR},
                {DoubleWithOutExitNoSkipAdvice.class, (double) BAR},
                {ReferenceWithOutExitNoSkipAdvice.class, FOO},
                {VoidWithOutExitNoSkipAdvice.class, null},
                {BooleanDelegateWithOutExitNoSkipAdvice.class, true},
                {ByteDelegateWithOutExitNoSkipAdvice.class, (byte) BAR},
                {ShortDelegateWithOutExitNoSkipAdvice.class, (short) BAR},
                {CharacterDelegateWithOutExitNoSkipAdvice.class, (char) BAR},
                {IntegerDelegateWithOutExitNoSkipAdvice.class, BAR},
                {LongDelegateWithOutExitNoSkipAdvice.class, (long) BAR},
                {FloatDelegateWithOutExitNoSkipAdvice.class, (float) BAR},
                {DoubleDelegateWithOutExitNoSkipAdvice.class, (double) BAR},
                {ReferenceDelegateWithOutExitNoSkipAdvice.class, FOO},
                {VoidDelegateWithOutExitNoSkipAdvice.class, null},
                {BooleanArrayAdvice.class, false},
                {ByteArrayAdvice.class, (byte) 0},
                {ShortArrayAdvice.class, (short) 0},
                {CharacterArrayAdvice.class, (char) 0},
                {IntegerArrayAdvice.class, 0},
                {LongArrayAdvice.class, 0L},
                {FloatArrayAdvice.class, 0f},
                {DoubleArrayAdvice.class, 0d},
                {ReferenceArrayAdvice.class, null},
                {BooleanArrayNoSkipAdvice.class, true},
                {ByteArrayNoSkipAdvice.class, (byte) BAR},
                {ShortArrayNoSkipAdvice.class, (short) BAR},
                {CharacterArrayNoSkipAdvice.class, (char) BAR},
                {IntegerArrayNoSkipAdvice.class, BAR},
                {LongArrayNoSkipAdvice.class, (long) BAR},
                {FloatArrayNoSkipAdvice.class, (float) BAR},
                {DoubleArrayNoSkipAdvice.class, (double) BAR},
                {ReferenceArrayNoSkipAdvice.class, FOO},
                {BooleanArrayNullAdvice.class, true},
                {ByteArrayNullAdvice.class, (byte) BAR},
                {ShortArrayNullAdvice.class, (short) BAR},
                {CharacterArrayNullAdvice.class, (char) BAR},
                {IntegerArrayNullAdvice.class, BAR},
                {LongArrayNullAdvice.class, (long) BAR},
                {FloatArrayNullAdvice.class, (float) BAR},
                {DoubleArrayNullAdvice.class, (double) BAR},
                {ReferenceArrayNullAdvice.class, FOO}
        });
    }

    private final Class<?> type;

    private final Object value;

    public AdviceSkipOnNonDefaultValueTest(Class<?> type, Object value) {
        this.type = type;
        this.value = value;
    }

    @Test
    public void testAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(this.type)
                .visit(Advice.to(this.type).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is(value));
    }

    @SuppressWarnings("unused")
    public static class BooleanAdvice {

        public boolean foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static boolean enter() {
            return true;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return boolean value) {
            if (value) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ByteAdvice {

        public byte foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static byte enter() {
            return BAR;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return byte value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ShortAdvice {

        public short foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static short enter() {
            return BAR;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return short value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterAdvice {

        public char foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static char enter() {
            return BAR;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return char value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerAdvice {

        public int foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static int enter() {
            return BAR;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return int value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class LongAdvice {

        public long foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static long enter() {
            return BAR;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return long value) {
            if (value != 0L) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FloatAdvice {

        public float foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static float enter() {
            return (float) BAR;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return float value) {
            if (value != 0f) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleAdvice {

        public double foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static double enter() {
            return BAR;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return double value) {
            if (value != 0d) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceAdvice {

        public Object foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static Object enter() {
            return new Object();
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return Object value) {
            if (value != null) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class VoidAdvice {

        public void foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static boolean enter() {
            return true;
        }

        @Advice.OnMethodExit
        private static void exit() {
            /* do nothing */
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegateAdvice {

        public boolean foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static boolean enter() {
            return true;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return boolean value) {
            if (value) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegateAdvice {

        public byte foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static byte enter() {
            return BAR;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return byte value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegateAdvice {

        public short foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static short enter() {
            return BAR;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return short value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegateAdvice {

        public char foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static char enter() {
            return BAR;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return char value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegateAdvice {

        public int foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static int enter() {
            return BAR;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return int value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegateAdvice {

        public long foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static long enter() {
            return BAR;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return long value) {
            if (value != 0L) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegateAdvice {

        public float foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static float enter() {
            return (float) BAR;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return float value) {
            if (value != 0f) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegateAdvice {

        public double foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static double enter() {
            return BAR;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return double value) {
            if (value != 0d) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegateAdvice {

        public Object foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static Object enter() {
            return new Object();
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return Object value) {
            if (value != null) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class VoidDelegateAdvice {

        public void foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static boolean enter() {
            return true;
        }

        @Advice.OnMethodExit
        private static void exit() {
            /* do nothing */
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanWithOutExitAdvice {

        public boolean foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteWithOutExitAdvice {

        public byte foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static byte enter() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortWithOutExitAdvice {

        public short foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static short enter() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterWithOutExitAdvice {

        public char foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static char enter() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerWithOutExitAdvice {

        public int foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static int enter() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class LongWithOutExitAdvice {

        public long foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static long enter() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatWithOutExitAdvice {

        public float foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static float enter() {
            return (float) BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleWithOutExitAdvice {

        public double foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static double enter() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceWithOutExitAdvice {

        public Object foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static Object enter() {
            return new Object();
        }
    }

    @SuppressWarnings("unused")
    public static class VoidWithOutExitAdvice {

        public void foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegateWithOutExitAdvice {

        public boolean foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegateWithOutExitAdvice {

        public byte foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static byte enter() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegateWithOutExitAdvice {

        public short foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static short enter() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegateWithOutExitAdvice {

        public char foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static char enter() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegateWithOutExitAdvice {

        public int foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static int enter() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegateWithOutExitAdvice {

        public long foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static long enter() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegateWithOutExitAdvice {

        public float foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static float enter() {
            return (float) BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegateWithOutExitAdvice {

        public double foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static double enter() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegateWithOutExitAdvice {

        public Object foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static Object enter() {
            return new Object();
        }
    }

    @SuppressWarnings("unused")
    public static class VoidDelegateWithOutExitAdvice {

        public void foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanNoSkipAdvice {

        public boolean foo() {
            return true;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static boolean enter() {
            return false;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return boolean value) {
            if (!value) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ByteNoSkipAdvice {

        public byte foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static byte enter() {
            return 0;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return byte value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ShortNoSkipAdvice {

        public short foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static short enter() {
            return 0;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return short value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterNoSkipAdvice {

        public char foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static char enter() {
            return 0;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return char value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerNoSkipAdvice {

        public int foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static int enter() {
            return 0;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return int value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class LongNoSkipAdvice {

        public long foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static long enter() {
            return 0L;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return long value) {
            if (value != (long) BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FloatNoSkipAdvice {

        public float foo() {
            return (float) BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static float enter() {
            return 0f;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return float value) {
            if (value != (float) BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleNoSkipAdvice {

        public double foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static double enter() {
            return 0d;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return double value) {
            if (value != (double) BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceNoSkipAdvice {

        public Object foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static Object enter() {
            return null;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return Object value) {
            if (!value.equals(FOO)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class VoidNoSkipAdvice {

        public void foo() {
            /* do nothing */
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static boolean enter() {
            return false;
        }

        @Advice.OnMethodExit
        private static void exit() {
            /* do nothing */
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegateNoSkipAdvice {

        public boolean foo() {
            return true;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static boolean enter() {
            return false;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return boolean value) {
            if (!value) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegateNoSkipAdvice {

        public byte foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static byte enter() {
            return 0;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return byte value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegateNoSkipAdvice {

        public short foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static short enter() {
            return 0;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return short value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegateNoSkipAdvice {

        public char foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static char enter() {
            return 0;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return char value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegateNoSkipAdvice {

        public int foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static int enter() {
            return 0;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return int value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegateNoSkipAdvice {

        public long foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static long enter() {
            return 0L;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return long value) {
            if (value != (long) BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegateNoSkipAdvice {

        public float foo() {
            return (float) BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static float enter() {
            return 0f;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return float value) {
            if (value != (float) BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegateNoSkipAdvice {

        public double foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static double enter() {
            return 0d;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return double value) {
            if (value != (double) BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegateNoSkipAdvice {

        public Object foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static Object enter() {
            return null;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return Object value) {
            if (!value.equals(FOO)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class VoidDelegateNoSkipAdvice {

        public void foo() {
            /* do nothing */
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static boolean enter() {
            return false;
        }

        @Advice.OnMethodExit
        private static void exit() {
            /* do nothing */
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanWithOutExitNoSkipAdvice {

        public boolean foo() {
            return true;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static boolean enter() {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteWithOutExitNoSkipAdvice {

        public byte foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static byte enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortWithOutExitNoSkipAdvice {

        public short foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static short enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterWithOutExitNoSkipAdvice {

        public char foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static char enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerWithOutExitNoSkipAdvice {

        public int foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static int enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class LongWithOutExitNoSkipAdvice {

        public long foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static long enter() {
            return 0L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatWithOutExitNoSkipAdvice {

        public float foo() {
            return (float) BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static float enter() {
            return 0f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleWithOutExitNoSkipAdvice {

        public double foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static double enter() {
            return 0d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceWithOutExitNoSkipAdvice {

        public Object foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static Object enter() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static class VoidWithOutExitNoSkipAdvice {

        public void foo() {
            /* do nothing */
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static boolean enter() {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegateWithOutExitNoSkipAdvice {

        public boolean foo() {
            return true;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static boolean enter() {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegateWithOutExitNoSkipAdvice {

        public byte foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static byte enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegateWithOutExitNoSkipAdvice {

        public short foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static short enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegateWithOutExitNoSkipAdvice {

        public char foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static char enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegateWithOutExitNoSkipAdvice {

        public int foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static int enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegateWithOutExitNoSkipAdvice {

        public long foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static long enter() {
            return 0L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegateWithOutExitNoSkipAdvice {

        public float foo() {
            return (float) BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static float enter() {
            return 0f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegateWithOutExitNoSkipAdvice {

        public double foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static double enter() {
            return 0d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegateWithOutExitNoSkipAdvice {

        public Object foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static Object enter() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static class VoidDelegateWithOutExitNoSkipAdvice {

        public void foo() {
            /* do nothing */
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, inline = false)
        private static boolean enter() {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanArrayAdvice {

        public boolean foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static boolean[] enter() {
            return new boolean[]{true};
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return boolean value) {
            if (value) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ByteArrayAdvice {

        public byte foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static byte[] enter() {
            return new byte[]{BAR};
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return byte value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ShortArrayAdvice {

        public short foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static short[] enter() {
            return new short[]{BAR};
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return short value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterArrayAdvice {

        public char foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static char[] enter() {
            return new char[]{BAR};
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return char value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerArrayAdvice {

        public int foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static int[] enter() {
            return new int[]{BAR};
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return int value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class LongArrayAdvice {

        public long foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static long[] enter() {
            return new long[]{BAR};
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return long value) {
            if (value != 0L) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FloatArrayAdvice {

        public float foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static float[] enter() {
            return new float[]{BAR};
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return float value) {
            if (value != 0f) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleArrayAdvice {

        public double foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static double[] enter() {
            return new double[]{BAR};
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return double value) {
            if (value != 0d) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceArrayAdvice {

        public Object foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static Object[] enter() {
            return new Object[]{new Object()};
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return Object value) {
            if (value != null) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanArrayNoSkipAdvice {

        public boolean foo() {
            return true;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static boolean[] enter() {
            return new boolean[1];
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return boolean value) {
            if (!value) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ByteArrayNoSkipAdvice {

        public byte foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static byte[] enter() {
            return new byte[1];
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return byte value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ShortArrayNoSkipAdvice {

        public short foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static short[] enter() {
            return new short[1];
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return short value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterArrayNoSkipAdvice {

        public char foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static char[] enter() {
            return new char[1];
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return char value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerArrayNoSkipAdvice {

        public int foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static int[] enter() {
            return new int[1];
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return int value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class LongArrayNoSkipAdvice {

        public long foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static long[] enter() {
            return new long[1];
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return long value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FloatArrayNoSkipAdvice {

        public float foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static float[] enter() {
            return new float[1];
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return float value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleArrayNoSkipAdvice {

        public double foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static double[] enter() {
            return new double[1];
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return double value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceArrayNoSkipAdvice {

        public Object foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static Object[] enter() {
            return new Object[1];
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return Object value) {
            if (value == null) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanArrayNullAdvice {

        public boolean foo() {
            return true;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static boolean[] enter() {
            return null;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return boolean value) {
            if (!value) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ByteArrayNullAdvice {

        public byte foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static byte[] enter() {
            return null;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return byte value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ShortArrayNullAdvice {

        public short foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static short[] enter() {
            return null;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return short value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterArrayNullAdvice {

        public char foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static char[] enter() {
            return null;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return char value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerArrayNullAdvice {

        public int foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static int[] enter() {
            return null;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return int value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class LongArrayNullAdvice {

        public long foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static long[] enter() {
            return null;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return long value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FloatArrayNullAdvice {

        public float foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static float[] enter() {
            return null;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return float value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleArrayNullAdvice {

        public double foo() {
            return BAR;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static double[] enter() {
            return null;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return double value) {
            if (value != BAR) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceArrayNullAdvice {

        public Object foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, skipOnIndex = 0)
        private static Object[] enter() {
            return null;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return Object value) {
            if (value == null) {
                throw new AssertionError();
            }
        }
    }
}
