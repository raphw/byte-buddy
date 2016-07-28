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
public class AdviceSkipIfTrueTypeTest {

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
                {VoidDelegateWithOutExitAdvice.class, null}
        });
    }

    private final Class<?> type;

    private final Object value;

    public AdviceSkipIfTrueTypeTest(Class<?> type, Object value) {
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
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is(value));
    }

    @SuppressWarnings("unused")
    public static class BooleanAdvice {

        public boolean foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true)
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

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true)
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

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
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

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
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

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
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

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteWithOutExitAdvice {

        public byte foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortWithOutExitAdvice {

        public short foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterWithOutExitAdvice {

        public char foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerWithOutExitAdvice {

        public int foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class LongWithOutExitAdvice {

        public long foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatWithOutExitAdvice {

        public float foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleWithOutExitAdvice {

        public double foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceWithOutExitAdvice {

        public Object foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class VoidWithOutExitAdvice {

        public void foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegateWithOutExitAdvice {

        public boolean foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegateWithOutExitAdvice {

        public byte foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegateWithOutExitAdvice {

        public short foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegateWithOutExitAdvice {

        public char foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegateWithOutExitAdvice {

        public int foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegateWithOutExitAdvice {

        public long foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegateWithOutExitAdvice {

        public float foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegateWithOutExitAdvice {

        public double foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegateWithOutExitAdvice {

        public Object foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class VoidDelegateWithOutExitAdvice {

        public void foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipIfTrue = true, inline = false)
        private static boolean enter() {
            return true;
        }
    }
}
