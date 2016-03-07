package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.ClassWriter;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

import static junit.framework.TestCase.fail;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceTypeTest {

    private static final String FOO = "foo", BAR = "bar", ENTER = "enter", EXIT = "exit", exception = "exception";

    private static final byte VALUE = 42;

    private static final boolean BOOLEAN = true;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BooleanAdvice.class, BOOLEAN},
                {ByteAdvice.class, VALUE},
                {ShortAdvice.class, (short) VALUE},
                {CharacterAdvice.class, (char) VALUE},
                {IntegerAdvice.class, (int) VALUE},
                {LongAdvice.class, (long) VALUE},
                {FloatAdvice.class, (float) VALUE},
                {DoubleAdvice.class, (double) VALUE},
                {ReferenceAdvice.class, FOO}
        });
    }

    private final Class<?> advice;

    private final Object expected;

    public AdviceTypeTest(Class<?> advice, Object expected) {
        this.advice = advice;
        this.expected = expected;
    }

    @Test
    public void testAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(advice)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(advice)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is(expected));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithException() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(advice)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(BAR), Advice.to(advice)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredField(exception).set(null, true);
        try {
            assertThat(type.getDeclaredMethod(BAR).invoke(type.newInstance()), is(expected));
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @SuppressWarnings("unused")
    public static class BooleanAdvice {

        public static int enter, exit;

        public static boolean exception;

        public boolean foo() {
            return BOOLEAN;
        }

        public boolean bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static boolean enter() {
            enter++;
            return BOOLEAN;
        }

        @Advice.OnMethodExit
        public static boolean exit(@Advice.Return boolean result, @Advice.Enter boolean enter, @Advice.Thrown Throwable throwable) {
            if (result == exception || !enter || (exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            exit++;
            return BOOLEAN;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteAdvice {

        public static int enter, exit;

        public static boolean exception;

        public byte foo() {
            return VALUE;
        }

        public byte bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static byte enter() {
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static byte exit(@Advice.Return byte result, @Advice.Enter byte enter, @Advice.Thrown Throwable throwable) {
            if (result != (exception ? 0 : VALUE) || enter != VALUE * 2 || (exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortAdvice {

        public static int enter, exit;

        public static boolean exception;

        public short foo() {
            return VALUE;
        }

        public short bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static short enter() {
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static short exit(@Advice.Return short result, @Advice.Enter short enter, @Advice.Thrown Throwable throwable) {
            if (result != (exception ? 0 : VALUE) || enter != VALUE * 2 || (exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterAdvice {

        public static int enter, exit;

        public static boolean exception;

        public char foo() {
            return VALUE;
        }

        public char bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static char enter() {
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static char exit(@Advice.Return char result, @Advice.Enter char enter, @Advice.Thrown Throwable throwable) {
            if (result != (exception ? 0 : VALUE) || enter != VALUE * 2 || (exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerAdvice {

        public static int enter, exit;

        public static boolean exception;

        public int foo() {
            return VALUE;
        }

        public int bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static int enter() {
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static int exit(@Advice.Return int result, @Advice.Enter int enter, @Advice.Thrown Throwable throwable) {
            if (result != (exception ? 0 : VALUE) || enter != VALUE * 2 || (exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class LongAdvice {

        public static int enter, exit;

        public static boolean exception;

        public long foo() {
            return VALUE;
        }

        public long bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static long enter() {
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static long exit(@Advice.Return long result, @Advice.Enter long enter, @Advice.Thrown Throwable throwable) {
            if (result != (exception ? 0 : VALUE) || enter != VALUE * 2 || (exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatAdvice {

        public static int enter, exit;

        public static boolean exception;

        public float foo() {
            return VALUE;
        }

        public float bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static float enter() {
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static float exit(@Advice.Return float result, @Advice.Enter float enter, @Advice.Thrown Throwable throwable) {
            if (result != (exception ? 0 : VALUE) || enter != VALUE * 2 || (exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleAdvice {

        public static int enter, exit;

        public static boolean exception;

        public double foo() {
            return VALUE;
        }

        public double bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static double enter() {
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static double exit(@Advice.Return double result, @Advice.Enter double enter, @Advice.Thrown Throwable throwable) {
            if (result != (exception ? 0 : VALUE) || enter != VALUE * 2 || (exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceAdvice {

        public static int enter, exit;

        public static boolean exception;

        public Object foo() {
            return FOO;
        }

        public Object bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static Object enter() {
            enter++;
            return FOO + BAR;
        }

        @Advice.OnMethodExit
        public static Object exit(@Advice.Return Object result, @Advice.Enter Object enter, @Advice.Thrown Throwable throwable) {
            if ((exception ? result != null : !result.equals(FOO))
                    || !enter.equals(FOO + BAR)
                    || (exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            exit++;
            return FOO;
        }
    }
}
