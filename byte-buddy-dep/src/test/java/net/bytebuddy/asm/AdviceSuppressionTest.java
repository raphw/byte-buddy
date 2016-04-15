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
                {BooleanInlineEnterAdvice.class},
                {ByteInlineEnterAdvice.class},
                {ShortInlineEnterAdvice.class},
                {CharacterInlineEnterAdvice.class},
                {IntegerInlineEnterAdvice.class},
                {LongInlineEnterAdvice.class},
                {FloatInlineEnterAdvice.class},
                {DoubleInlineEnterAdvice.class},
                {ReferenceInlineEnterAdvice.class},
                {BooleanDelegationEnterAdvice.class},
                {ByteDelegationEnterAdvice.class},
                {ShortDelegationEnterAdvice.class},
                {CharacterDelegationEnterAdvice.class},
                {IntegerDelegationEnterAdvice.class},
                {LongDelegationEnterAdvice.class},
                {FloatDelegationEnterAdvice.class},
                {DoubleDelegationEnterAdvice.class},
                {ReferenceDelegationEnterAdvice.class},
                {BooleanInlineExitAdvice.class},
                {ByteInlineExitAdvice.class},
                {ShortInlineExitAdvice.class},
                {CharacterInlineExitAdvice.class},
                {IntegerInlineExitAdvice.class},
                {LongInlineExitAdvice.class},
                {FloatInlineExitAdvice.class},
                {DoubleInlineExitAdvice.class},
                {ReferenceInlineExitAdvice.class},
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
        assertThat(dynamicType.getDeclaredMethod(FOO).invoke(dynamicType.newInstance()), is((Object) FOO));
    }

    @SuppressWarnings("unused")
    public static class BooleanInlineEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static boolean exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ByteInlineEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static byte exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ShortInlineEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static short exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterInlineEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static byte exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerInlineEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static int exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class LongInlineEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static long exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class FloatInlineEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static float exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleInlineEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static double exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceInlineEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        public static Object exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegationEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static boolean exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegationEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static byte exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegationEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static short exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegationEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static byte exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegationEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static int exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegationEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static long exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegationEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static float exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegationEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static double exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegationEnterAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class, inline = false)
        public static Object exit() {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanInlineExitAdvice {

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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
    public static class BooleanDelegationExitAdvice {

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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

        public String foo() {
            return FOO;
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
