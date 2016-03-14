package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Parameterized.class)
public class AdviceSuppressionTest {

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
                {ReferenceAdvice.class}
        });
    }

    private final Class<?> type;

    public AdviceSuppressionTest(Class<?> type) {
        this.type = type;
    }

    @Test
    public void testIllegalAssignment() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(type).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethod(FOO).invoke(dynamicType.newInstance()), is((Object) FOO));
    }

    public static class Sample {

        public String foo() {
            return FOO;
        }
    }

    public static class BooleanAdvice {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        public static boolean enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        public static void exit(@Advice.Enter boolean value) {
            if (value) {
                throw new AssertionError();
            }
        }
    }

    public static class ByteAdvice {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        public static byte enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter byte value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    public static class ShortAdvice {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        public static short enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter short value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    public static class CharacterAdvice {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        public static byte enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter byte value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    public static class IntegerAdvice {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        public static int enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter int value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    public static class LongAdvice {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        public static long enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter long value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    public static class FloatAdvice {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        public static float enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter float value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    public static class DoubleAdvice {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        public static double enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter double value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    public static class ReferenceAdvice {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        public static Object enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter Object value) {
            if (value != null) {
                throw new AssertionError();
            }
        }
    }

}
