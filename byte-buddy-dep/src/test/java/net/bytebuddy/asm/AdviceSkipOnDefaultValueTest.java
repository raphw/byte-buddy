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
public class AdviceSkipOnDefaultValueTest {

    private static final String FOO = "foo";

    @Parameterized.Parameters
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
                {ByteNoSkipAdvice.class, (byte) 42},
                {ShortNoSkipAdvice.class, (short) 42},
                {CharacterNoSkipAdvice.class, (char) 42},
                {IntegerNoSkipAdvice.class, 42},
                {LongNoSkipAdvice.class, 42L},
                {FloatNoSkipAdvice.class, 42f},
                {DoubleNoSkipAdvice.class, 42d},
                {ReferenceNoSkipAdvice.class, FOO},
                {VoidNoSkipAdvice.class, null},
                {BooleanDelegateNoSkipAdvice.class, true},
                {ByteDelegateNoSkipAdvice.class, (byte) 42},
                {ShortDelegateNoSkipAdvice.class, (short) 42},
                {CharacterDelegateNoSkipAdvice.class, (char) 42},
                {IntegerDelegateNoSkipAdvice.class, 42},
                {LongDelegateNoSkipAdvice.class, 42L},
                {FloatDelegateNoSkipAdvice.class, 42f},
                {DoubleDelegateNoSkipAdvice.class, 42d},
                {ReferenceDelegateNoSkipAdvice.class, FOO},
                {VoidDelegateNoSkipAdvice.class, null},
                {BooleanWithOutExitNoSkipAdvice.class, true},
                {ByteWithOutExitNoSkipAdvice.class, (byte) 42},
                {ShortWithOutExitNoSkipAdvice.class, (short) 42},
                {CharacterWithOutExitNoSkipAdvice.class, (char) 42},
                {IntegerWithOutExitNoSkipAdvice.class, 42},
                {LongWithOutExitNoSkipAdvice.class, 42L},
                {FloatWithOutExitNoSkipAdvice.class, 42f},
                {DoubleWithOutExitNoSkipAdvice.class, 42d},
                {ReferenceWithOutExitNoSkipAdvice.class, FOO},
                {VoidWithOutExitNoSkipAdvice.class, null},
                {BooleanDelegateWithOutExitNoSkipAdvice.class, true},
                {ByteDelegateWithOutExitNoSkipAdvice.class, (byte) 42},
                {ShortDelegateWithOutExitNoSkipAdvice.class, (short) 42},
                {CharacterDelegateWithOutExitNoSkipAdvice.class, (char) 42},
                {IntegerDelegateWithOutExitNoSkipAdvice.class, 42},
                {LongDelegateWithOutExitNoSkipAdvice.class, 42L},
                {FloatDelegateWithOutExitNoSkipAdvice.class, 42f},
                {DoubleDelegateWithOutExitNoSkipAdvice.class, 42d},
                {ReferenceDelegateWithOutExitNoSkipAdvice.class, FOO},
                {VoidDelegateWithOutExitNoSkipAdvice.class, null}
        });
    }

    private final Class<?> type;

    private final Object value;

    public AdviceSkipOnDefaultValueTest(Class<?> type, Object value) {
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static boolean enter() {
            return false;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static byte enter() {
            return 0;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static short enter() {
            return 0;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static char enter() {
            return 0;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static int enter() {
            return 0;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static long enter() {
            return 0L;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static float enter() {
            return 0f;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static double enter() {
            return 0d;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static Object enter() {
            return null;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return Object value) {
            if (value != null) {
                throw new AssertionError("Equality");
            }
        }
    }

    @SuppressWarnings("unused")
    public static class VoidAdvice {

        public void foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static boolean enter() {
            return false;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static boolean enter() {
            return false;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static byte enter() {
            return 0;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static short enter() {
            return 0;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static char enter() {
            return 0;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static int enter() {
            return 0;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static long enter() {
            return 0L;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static float enter() {
            return 0f;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static double enter() {
            return 0d;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static Object enter() {
            return null;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static boolean enter() {
            return false;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static boolean enter() {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteWithOutExitAdvice {

        public byte foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static byte enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortWithOutExitAdvice {

        public short foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static short enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterWithOutExitAdvice {

        public char foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static char enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerWithOutExitAdvice {

        public int foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static int enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class LongWithOutExitAdvice {

        public long foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static long enter() {
            return 0L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatWithOutExitAdvice {

        public float foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static float enter() {
            return 0f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleWithOutExitAdvice {

        public double foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static double enter() {
            return 0d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceWithOutExitAdvice {

        public Object foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static Object enter() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static class VoidWithOutExitAdvice {

        public void foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static boolean enter() {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegateWithOutExitAdvice {

        public boolean foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static boolean enter() {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegateWithOutExitAdvice {

        public byte foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static byte enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegateWithOutExitAdvice {

        public short foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static short enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegateWithOutExitAdvice {

        public char foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static char enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegateWithOutExitAdvice {

        public int foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static int enter() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegateWithOutExitAdvice {

        public long foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static long enter() {
            return 0L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegateWithOutExitAdvice {

        public float foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static float enter() {
            return 0f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegateWithOutExitAdvice {

        public double foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static double enter() {
            return 0d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegateWithOutExitAdvice {

        public Object foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static Object enter() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static class VoidDelegateWithOutExitAdvice {

        public void foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static boolean enter() {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanNoSkipAdvice {

        public boolean foo() {
            return true;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static boolean enter() {
            return true;
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
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static byte enter() {
            return 42;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return byte value) {
            if (value != 42) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ShortNoSkipAdvice {

        public short foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static short enter() {
            return 42;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return short value) {
            if (value != 42) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterNoSkipAdvice {

        public char foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static char enter() {
            return 42;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return char value) {
            if (value != 42) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerNoSkipAdvice {

        public int foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static int enter() {
            return 42;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return int value) {
            if (value != 42) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class LongNoSkipAdvice {

        public long foo() {
            return 42L;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static long enter() {
            return 42L;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return long value) {
            if (value != 42L) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FloatNoSkipAdvice {

        public float foo() {
            return 42f;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static float enter() {
            return 42f;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return float value) {
            if (value != 42f) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleNoSkipAdvice {

        public double foo() {
            return 42d;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static double enter() {
            return 42d;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return double value) {
            if (value != 42d) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceNoSkipAdvice {

        public Object foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static Object enter() {
            return new Object();
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static boolean enter() {
            return true;
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
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static byte enter() {
            return 42;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return byte value) {
            if (value != 42) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegateNoSkipAdvice {

        public short foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static short enter() {
            return 42;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return short value) {
            if (value != 42) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegateNoSkipAdvice {

        public char foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static char enter() {
            return 42;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return char value) {
            if (value != 42) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegateNoSkipAdvice {

        public int foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static int enter() {
            return 42;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return int value) {
            if (value != 42) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegateNoSkipAdvice {

        public long foo() {
            return 42L;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static long enter() {
            return 42L;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return long value) {
            if (value != 42L) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegateNoSkipAdvice {

        public float foo() {
            return 42f;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static float enter() {
            return 42f;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return float value) {
            if (value != 42f) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegateNoSkipAdvice {

        public double foo() {
            return 42d;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static double enter() {
            return 42d;
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return double value) {
            if (value != 42d) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegateNoSkipAdvice {

        public Object foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static Object enter() {
            return new Object();
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteWithOutExitNoSkipAdvice {

        public byte foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static byte enter() {
            return 42;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortWithOutExitNoSkipAdvice {

        public short foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static short enter() {
            return 42;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterWithOutExitNoSkipAdvice {

        public char foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static char enter() {
            return 42;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerWithOutExitNoSkipAdvice {

        public int foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static int enter() {
            return 42;
        }
    }

    @SuppressWarnings("unused")
    public static class LongWithOutExitNoSkipAdvice {

        public long foo() {
            return 42L;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static long enter() {
            return 42L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatWithOutExitNoSkipAdvice {

        public float foo() {
            return 42f;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static float enter() {
            return 42f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleWithOutExitNoSkipAdvice {

        public double foo() {
            return 42d;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static double enter() {
            return 42d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceWithOutExitNoSkipAdvice {

        public Object foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static Object enter() {
            return new Object();
        }
    }

    @SuppressWarnings("unused")
    public static class VoidWithOutExitNoSkipAdvice {

        public void foo() {
            /* do nothing */
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegateWithOutExitNoSkipAdvice {

        public boolean foo() {
            return true;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegateWithOutExitNoSkipAdvice {

        public byte foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static byte enter() {
            return 42;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegateWithOutExitNoSkipAdvice {

        public short foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static short enter() {
            return 42;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegateWithOutExitNoSkipAdvice {

        public char foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static char enter() {
            return 42;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegateWithOutExitNoSkipAdvice {

        public int foo() {
            return 42;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static int enter() {
            return 42;
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegateWithOutExitNoSkipAdvice {

        public long foo() {
            return 42L;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static long enter() {
            return 42L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegateWithOutExitNoSkipAdvice {

        public float foo() {
            return 42f;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static float enter() {
            return 42f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegateWithOutExitNoSkipAdvice {

        public double foo() {
            return 42d;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static double enter() {
            return 42d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegateWithOutExitNoSkipAdvice {

        public Object foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static Object enter() {
            return new Object();
        }
    }

    @SuppressWarnings("unused")
    public static class VoidDelegateWithOutExitNoSkipAdvice {

        public void foo() {
            /* do nothing */
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, inline = false)
        private static boolean enter() {
            return true;
        }
    }
}
