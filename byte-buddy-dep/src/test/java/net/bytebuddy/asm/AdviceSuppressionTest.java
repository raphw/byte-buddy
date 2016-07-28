package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceSuppressionTest {

    private static final String FOO = "foo";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {VoidInlineEnterAdvice.class},
                {BooleanInlineEnterAdvice.class},
                {ByteInlineEnterAdvice.class},
                {ShortInlineEnterAdvice.class},
                {CharacterInlineEnterAdvice.class},
                {IntegerInlineEnterAdvice.class},
                {LongInlineEnterAdvice.class},
                {FloatInlineEnterAdvice.class},
                {DoubleInlineEnterAdvice.class},
                {ReferenceInlineEnterAdvice.class},
                {VoidDelegationEnterAdvice.class},
                {BooleanDelegationEnterAdvice.class},
                {ByteDelegationEnterAdvice.class},
                {ShortDelegationEnterAdvice.class},
                {CharacterDelegationEnterAdvice.class},
                {IntegerDelegationEnterAdvice.class},
                {LongDelegationEnterAdvice.class},
                {FloatDelegationEnterAdvice.class},
                {DoubleDelegationEnterAdvice.class},
                {ReferenceDelegationEnterAdvice.class},
                {VoidInlineExitAdvice.class},
                {BooleanInlineExitAdvice.class},
                {ByteInlineExitAdvice.class},
                {ShortInlineExitAdvice.class},
                {CharacterInlineExitAdvice.class},
                {IntegerInlineExitAdvice.class},
                {LongInlineExitAdvice.class},
                {FloatInlineExitAdvice.class},
                {DoubleInlineExitAdvice.class},
                {ReferenceInlineExitAdvice.class},
                {VoidDelegationExitAdvice.class},
                {BooleanDelegationExitAdvice.class},
                {ByteDelegationExitAdvice.class},
                {ShortDelegationExitAdvice.class},
                {CharacterDelegationExitAdvice.class},
                {IntegerDelegationExitAdvice.class},
                {LongDelegationExitAdvice.class},
                {FloatDelegationExitAdvice.class},
                {DoubleDelegationExitAdvice.class},
                {ReferenceDelegationExitAdvice.class}
        });
    }

    private final Class<?> type;

    public AdviceSuppressionTest(Class<?> type) {
        this.type = type;
    }

    @Test
    public void testIllegalAssignment() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(type).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethod(FOO).invoke(dynamicType.getConstructor().newInstance()), nullValue(Object.class));
    }

    @SuppressWarnings("unused")
    public static class VoidInlineEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static void exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanInlineEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static boolean exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ByteInlineEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static byte exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ShortInlineEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static short exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterInlineEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static byte exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerInlineEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static int exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class LongInlineEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static long exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class FloatInlineEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static float exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleInlineEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static double exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceInlineEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static Object exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegationEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static boolean exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class VoidDelegationEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static void exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegationEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static byte exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegationEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static short exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegationEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static byte exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegationEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static int exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegationEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static long exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegationEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static float exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegationEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static double exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegationEnterAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static Object exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class VoidInlineExitAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        public static void enter() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanInlineExitAdvice {

        public void foo() {
            /* empty */
        }

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

    @SuppressWarnings("unused")
    public static class ByteInlineExitAdvice {

        public void foo() {
            /* empty */
        }

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

    @SuppressWarnings("unused")
    public static class ShortInlineExitAdvice {

        public void foo() {
            /* empty */
        }

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

    @SuppressWarnings("unused")
    public static class CharacterInlineExitAdvice {

        public void foo() {
            /* empty */
        }

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

    @SuppressWarnings("unused")
    public static class IntegerInlineExitAdvice {

        public void foo() {
            /* empty */
        }

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

    @SuppressWarnings("unused")
    public static class LongInlineExitAdvice {

        public void foo() {
            /* empty */
        }

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

    @SuppressWarnings("unused")
    public static class FloatInlineExitAdvice {

        public void foo() {
            /* empty */
        }

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

    @SuppressWarnings("unused")
    public static class DoubleInlineExitAdvice {

        public void foo() {
            /* empty */
        }

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

    @SuppressWarnings("unused")
    public static class ReferenceInlineExitAdvice {

        public void foo() {
            /* empty */
        }

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

    @SuppressWarnings("unused")
    public static class VoidDelegationExitAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter(suppress = RuntimeException.class, inline = false)
        public static void enter() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegationExitAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter(suppress = RuntimeException.class, inline = false)
        public static boolean enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(inline = false)
        @SuppressWarnings("all")
        public static void exit(@Advice.Enter boolean value) {
            if (value) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegationExitAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter(suppress = RuntimeException.class, inline = false)
        public static byte enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(inline = false)
        public static void exit(@Advice.Enter byte value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegationExitAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter(suppress = RuntimeException.class, inline = false)
        public static short enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(inline = false)
        public static void exit(@Advice.Enter short value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegationExitAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter(suppress = RuntimeException.class, inline = false)
        public static byte enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(inline = false)
        public static void exit(@Advice.Enter byte value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegationExitAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter(suppress = RuntimeException.class, inline = false)
        public static int enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(inline = false)
        public static void exit(@Advice.Enter int value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegationExitAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter(suppress = RuntimeException.class, inline = false)
        public static long enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(inline = false)
        public static void exit(@Advice.Enter long value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegationExitAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter(suppress = RuntimeException.class, inline = false)
        public static float enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(inline = false)
        public static void exit(@Advice.Enter float value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegationExitAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter(suppress = RuntimeException.class, inline = false)
        public static double enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(inline = false)
        public static void exit(@Advice.Enter double value) {
            if (value != 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegationExitAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter(suppress = RuntimeException.class, inline = false)
        public static Object enter() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(inline = false)
        public static void exit(@Advice.Enter Object value) {
            if (value != null) {
                throw new AssertionError();
            }
        }
    }
}
