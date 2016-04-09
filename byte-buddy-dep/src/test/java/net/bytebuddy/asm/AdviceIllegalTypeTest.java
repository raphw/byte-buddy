package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;

@RunWith(Parameterized.class)
public class AdviceIllegalTypeTest {

    private static final String FOO = "foo";

    private static final byte VALUE = 42;

    private static final boolean BOOLEAN = true;

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
                {ReferenceAdvice.class}
        });
    }

    private final Class<?> type;

    public AdviceIllegalTypeTest(Class<?> type) {
        this.type = type;
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalAssignment() throws Exception {
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(type).on(named(FOO)))
                .make();
    }

    public static class BooleanAdvice {

        void foo(boolean value) {
            /* empty */
        }

        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(0) boolean value) {
            value = BOOLEAN;
        }
    }

    public static class ByteAdvice {

        void foo(byte value) {
            /* empty */
        }

        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(0) byte value) {
            value = VALUE;
        }
    }

    public static class ShortAdvice {

        void foo(short value) {
            /* empty */
        }

        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(0) short value) {
            value = VALUE;
        }
    }

    public static class CharacterAdvice {

        void foo(char value) {
            /* empty */
        }

        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(0) char value) {
            value = VALUE;
        }
    }

    public static class IntegerAdvice {

        void foo(int value) {
            /* empty */
        }

        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(0) int value) {
            value = VALUE;
        }
    }

    public static class LongAdvice {

        void foo(long value) {
            /* empty */
        }

        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(0) long value) {
            value = VALUE;
        }
    }

    public static class FloatAdvice {

        void foo(float value) {
            /* empty */
        }

        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(0) float value) {
            value = VALUE;
        }
    }

    public static class DoubleAdvice {

        void foo(double value) {
            /* empty */
        }

        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(0) double value) {
            value = VALUE;
        }
    }

    public static class ReferenceAdvice {

        void foo(Object value) {
            /* empty */
        }

        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(0) Object value) {
            value = FOO;
        }
    }
}
