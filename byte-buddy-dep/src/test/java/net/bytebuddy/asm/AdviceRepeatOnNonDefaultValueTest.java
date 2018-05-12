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
public class AdviceRepeatOnNonDefaultValueTest {

    private static final String FOO = "foo";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BooleanAdvice.class},
                {ByteAdvice.class},
                {ShortAdvice.class},
                {CharacterAdvice.class},
                {IntegerAdvice.class},
                {LongAdvice.class},
                {FloatAdvice.class},
                {DoubleAdvice.class},
                {ReferenceAdvice.class},
                {BooleanAdviceWithoutArgumentBackup.class},
                {ByteAdviceWithoutArgumentBackup.class},
                {ShortAdviceWithoutArgumentBackup.class},
                {CharacterAdviceWithoutArgumentBackup.class},
                {IntegerAdviceWithoutArgumentBackup.class},
                {LongAdviceWithoutArgumentBackup.class},
                {FloatAdviceWithoutArgumentBackup.class},
                {DoubleAdviceWithoutArgumentBackup.class},
                {ReferenceAdviceWithoutArgumentBackup.class},
                {BooleanAdviceWithEnterAdvice.class},
                {ByteAdviceWithEnterAdvice.class},
                {ShortAdviceWithEnterAdvice.class},
                {CharacterAdviceWithEnterAdvice.class},
                {IntegerAdviceWithEnterAdvice.class},
                {LongAdviceWithEnterAdvice.class},
                {FloatAdviceWithEnterAdvice.class},
                {DoubleAdviceWithEnterAdvice.class},
                {ReferenceAdviceWithEnterAdvice.class},
                {BooleanAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {ByteAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {ShortAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {CharacterAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {IntegerAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {LongAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {FloatAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {DoubleAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {ReferenceAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {BooleanDelegatingAdvice.class},
                {ByteDelegatingAdvice.class},
                {ShortDelegatingAdvice.class},
                {CharacterDelegatingAdvice.class},
                {IntegerDelegatingAdvice.class},
                {LongDelegatingAdvice.class},
                {FloatDelegatingAdvice.class},
                {DoubleDelegatingAdvice.class},
                {ReferenceDelegatingAdvice.class},
                {BooleanDelegatingAdviceWithoutArgumentBackup.class},
                {ByteDelegatingAdviceWithoutArgumentBackup.class},
                {ShortDelegatingAdviceWithoutArgumentBackup.class},
                {CharacterDelegatingAdviceWithoutArgumentBackup.class},
                {IntegerDelegatingAdviceWithoutArgumentBackup.class},
                {LongDelegatingAdviceWithoutArgumentBackup.class},
                {FloatDelegatingAdviceWithoutArgumentBackup.class},
                {DoubleDelegatingAdviceWithoutArgumentBackup.class},
                {ReferenceDelegatingAdviceWithoutArgumentBackup.class},
                {BooleanDelegatingAdviceWithEnterAdvice.class},
                {ByteDelegatingAdviceWithEnterAdvice.class},
                {ShortDelegatingAdviceWithEnterAdvice.class},
                {CharacterDelegatingAdviceWithEnterAdvice.class},
                {IntegerDelegatingAdviceWithEnterAdvice.class},
                {LongDelegatingAdviceWithEnterAdvice.class},
                {FloatDelegatingAdviceWithEnterAdvice.class},
                {DoubleDelegatingAdviceWithEnterAdvice.class},
                {ReferenceDelegatingAdviceWithEnterAdvice.class},
                {BooleanDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {ByteDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {ShortDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {CharacterDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {IntegerDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {LongDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {FloatDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {DoubleDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
                {ReferenceDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup.class},
        });
    }

    private final Class<?> type;

    public AdviceRepeatOnNonDefaultValueTest(Class<?> type) {
        this.type = type;
    }

    @Test
    public void testAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(this.type)
                .visit(Advice.to(this.type).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is(3));
    }

    @SuppressWarnings("unused")
    public static class BooleanAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static boolean exit(@Advice.Return int count) {
            return count < 3;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static byte exit(@Advice.Return int count) {
            return (byte) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class ShortAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static short exit(@Advice.Return int count) {
            return (short) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static char exit(@Advice.Return int count) {
            return (char) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static int exit(@Advice.Return int count) {
            return count < 3 ? 1 : 0;
        }
    }

    @SuppressWarnings("unused")
    public static class LongAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static long exit(@Advice.Return int count) {
            return count < 3 ? 1L : 0L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static float exit(@Advice.Return int count) {
            return count < 3 ? 1f : 0f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static double exit(@Advice.Return int count) {
            return count < 3 ? 1d : 0d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static Object exit(@Advice.Return int count) {
            return count < 3 ? FOO : null;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false)
        private static boolean exit(@Advice.Return int count) {
            return count < 3;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false)
        private static byte exit(@Advice.Return int count) {
            return (byte) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class ShortAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false)
        private static short exit(@Advice.Return int count) {
            return (short) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false)
        private static char exit(@Advice.Return int count) {
            return (char) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false)
        private static int exit(@Advice.Return int count) {
            return count < 3 ? 1 : 0;
        }
    }

    @SuppressWarnings("unused")
    public static class LongAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false)
        private static long exit(@Advice.Return int count) {
            return count < 3 ? 1L : 0L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false)
        private static float exit(@Advice.Return int count) {
            return count < 3 ? 1f : 0f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false)
        private static double exit(@Advice.Return int count) {
            return count < 3 ? 1d : 0d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false)
        private static Object exit(@Advice.Return int count) {
            return count < 3 ? FOO : null;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static boolean enter() {
            return false;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static boolean exit(@Advice.Return int count) {
            return count < 3;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static byte enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static byte exit(@Advice.Return int count) {
            return (byte) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class ShortAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static short enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static short exit(@Advice.Return int count) {
            return (short) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static char enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static char exit(@Advice.Return int count) {
            return (char) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static int enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static int exit(@Advice.Return int count) {
            return count < 3 ? 1 : 0;
        }
    }

    @SuppressWarnings("unused")
    public static class LongAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static long enter() {
            return 0L;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static long exit(@Advice.Return int count) {
            return count < 3 ? 1L : 0L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static float enter() {
            return 0f;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static float exit(@Advice.Return int count) {
            return count < 3 ? 1f : 0f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static double enter() {
            return 0d;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static double exit(@Advice.Return int count) {
            return count < 3 ? 1d : 0d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static Object enter() {
            return null;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static Object exit(@Advice.Return int count) {
            return count < 3 ? FOO : null;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static boolean enter() {
            return false;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static boolean exit(@Advice.Return int count) {
            return count < 3;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static byte enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static byte exit(@Advice.Return int count) {
            return (byte) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class ShortAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static short enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static short exit(@Advice.Return int count) {
            return (short) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static char enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static char exit(@Advice.Return int count) {
            return (char) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static int enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static int exit(@Advice.Return int count) {
            return count < 3 ? 1 : 0;
        }
    }

    @SuppressWarnings("unused")
    public static class LongAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static long enter() {
            return 0L;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static long exit(@Advice.Return int count) {
            return count < 3 ? 1L : 0L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static float enter() {
            return 0f;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static float exit(@Advice.Return int count) {
            return count < 3 ? 1f : 0f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static double enter() {
            return 0d;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static double exit(@Advice.Return int count) {
            return count < 3 ? 1d : 0d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter
        private static Object enter() {
            return null;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static Object exit(@Advice.Return int count) {
            return count < 3 ? FOO : null;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegatingAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static boolean exit(@Advice.Return int count) {
            return count < 3;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegatingAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static byte exit(@Advice.Return int count) {
            return (byte) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegatingAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static short exit(@Advice.Return int count) {
            return (short) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegatingAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static char exit(@Advice.Return int count) {
            return (char) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegatingAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static int exit(@Advice.Return int count) {
            return count < 3 ? 1 : 0;
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegatingAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static long exit(@Advice.Return int count) {
            return count < 3 ? 1L : 0L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegatingAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static float exit(@Advice.Return int count) {
            return count < 3 ? 1f : 0f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegatingAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static double exit(@Advice.Return int count) {
            return count < 3 ? 1d : 0d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegatingAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static Object exit(@Advice.Return int count) {
            return count < 3 ? FOO : null;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegatingAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false, inline = false)
        private static boolean exit(@Advice.Return int count) {
            return count < 3;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegatingAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false, inline = false)
        private static byte exit(@Advice.Return int count) {
            return (byte) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegatingAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false, inline = false)
        private static short exit(@Advice.Return int count) {
            return (short) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegatingAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false, inline = false)
        private static char exit(@Advice.Return int count) {
            return (char) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegatingAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false, inline = false)
        private static int exit(@Advice.Return int count) {
            return count < 3 ? 1 : 0;
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegatingAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false, inline = false)
        private static long exit(@Advice.Return int count) {
            return count < 3 ? 1L : 0L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegatingAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false, inline = false)
        private static float exit(@Advice.Return int count) {
            return count < 3 ? 1f : 0f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegatingAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false, inline = false)
        private static double exit(@Advice.Return int count) {
            return count < 3 ? 1d : 0d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegatingAdviceWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, backupArguments = false, inline = false)
        private static Object exit(@Advice.Return int count) {
            return count < 3 ? FOO : null;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegatingAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static boolean enter() {
            return false;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static boolean exit(@Advice.Return int count) {
            return count < 3;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegatingAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static byte enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static byte exit(@Advice.Return int count) {
            return (byte) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegatingAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static short enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static short exit(@Advice.Return int count) {
            return (short) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegatingAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static char enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static char exit(@Advice.Return int count) {
            return (char) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegatingAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static int enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static int exit(@Advice.Return int count) {
            return count < 3 ? 1 : 0;
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegatingAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static long enter() {
            return 0L;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static long exit(@Advice.Return int count) {
            return count < 3 ? 1L : 0L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegatingAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static float enter() {
            return 0f;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static float exit(@Advice.Return int count) {
            return count < 3 ? 1f : 0f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegatingAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static double enter() {
            return 0d;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static double exit(@Advice.Return int count) {
            return count < 3 ? 1d : 0d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegatingAdviceWithEnterAdvice {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static Object enter() {
            return null;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static Object exit(@Advice.Return int count) {
            return count < 3 ? FOO : null;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static boolean enter() {
            return false;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static boolean exit(@Advice.Return int count) {
            return count < 3;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static byte enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static byte exit(@Advice.Return int count) {
            return (byte) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static short enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static short exit(@Advice.Return int count) {
            return (short) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static char enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static char exit(@Advice.Return int count) {
            return (char) (count < 3 ? 1 : 0);
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static int enter() {
            return 0;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static int exit(@Advice.Return int count) {
            return count < 3 ? 1 : 0;
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static long enter() {
            return 0L;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static long exit(@Advice.Return int count) {
            return count < 3 ? 1L : 0L;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static float enter() {
            return 0f;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static float exit(@Advice.Return int count) {
            return count < 3 ? 1f : 0f;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static double enter() {
            return 0d;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static double exit(@Advice.Return int count) {
            return count < 3 ? 1d : 0d;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegatingAdviceWithEnterAdviceAndWithoutArgumentBackup {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodEnter(inline = false)
        private static Object enter() {
            return null;
        }

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class, inline = false)
        private static Object exit(@Advice.Return int count) {
            return count < 3 ? FOO : null;
        }
    }
}
