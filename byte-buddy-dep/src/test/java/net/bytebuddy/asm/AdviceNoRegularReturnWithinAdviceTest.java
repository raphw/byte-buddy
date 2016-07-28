package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

import static junit.framework.TestCase.fail;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceNoRegularReturnWithinAdviceTest {

    private static final String FOO = "foo";

    private final Class<?> type;

    public AdviceNoRegularReturnWithinAdviceTest(Class<?> type) {
        this.type = type;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {VoidEnterAdvice.class},
                {BooleanEnterAdvice.class},
                {BooleanEnterSkipAdvice.class},
                {ByteEnterAdvice.class},
                {ShortEnterAdvice.class},
                {CharacterEnterAdvice.class},
                {IntegerEnterAdvice.class},
                {LongEnterAdvice.class},
                {FloatEnterAdvice.class},
                {DoubleEnterAdvice.class},
                {ReferenceEnterAdvice.class},
                {VoidExitAdvice.class},
                {BooleanExitAdvice.class},
                {ByteExitAdvice.class},
                {ShortExitAdvice.class},
                {CharacterExitAdvice.class},
                {IntegerExitAdvice.class},
                {LongExitAdvice.class},
                {FloatExitAdvice.class},
                {DoubleExitAdvice.class},
                {ReferenceExitAdvice.class},
                {VoidExitHandlerAdvice.class},
                {BooleanExitHandlerAdvice.class},
                {ByteExitHandlerAdvice.class},
                {ShortExitHandlerAdvice.class},
                {CharacterExitHandlerAdvice.class},
                {IntegerExitHandlerAdvice.class},
                {LongExitHandlerAdvice.class},
                {FloatExitHandlerAdvice.class},
                {DoubleExitHandlerAdvice.class},
                {ReferenceExitHandlerAdvice.class}
        });
    }

    @Test
    public void testNoRegularReturn() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(this.type).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
    }

    public static class Sample {

        public void foo() {
            /* empty */
        }
    }

    private static class VoidEnterAdvice {

        @Advice.OnMethodEnter
        public static void foo() {
            throw new RuntimeException();
        }
    }

    private static class BooleanEnterAdvice {

        @Advice.OnMethodEnter
        public static boolean foo() {
            throw new RuntimeException();
        }
    }

    private static class BooleanEnterSkipAdvice {

        @Advice.OnMethodEnter(skipIfTrue = true)
        public static boolean foo() {
            throw new RuntimeException();
        }
    }

    private static class ByteEnterAdvice {

        @Advice.OnMethodEnter
        public static byte foo() {
            throw new RuntimeException();
        }
    }

    private static class ShortEnterAdvice {

        @Advice.OnMethodEnter
        public static short foo() {
            throw new RuntimeException();
        }
    }

    private static class CharacterEnterAdvice {

        @Advice.OnMethodEnter
        public static char foo() {
            throw new RuntimeException();
        }
    }

    private static class IntegerEnterAdvice {

        @Advice.OnMethodEnter
        public static int foo() {
            throw new RuntimeException();
        }
    }

    private static class LongEnterAdvice {

        @Advice.OnMethodEnter
        public static long foo() {
            throw new RuntimeException();
        }
    }

    private static class FloatEnterAdvice {

        @Advice.OnMethodEnter
        public static float foo() {
            throw new RuntimeException();
        }
    }

    private static class DoubleEnterAdvice {

        @Advice.OnMethodEnter
        public static double foo() {
            throw new RuntimeException();
        }
    }

    private static class ReferenceEnterAdvice {

        @Advice.OnMethodEnter
        public static Object foo() {
            throw new RuntimeException();
        }
    }

    private static class VoidExitAdvice {

        @Advice.OnMethodExit
        public static void foo() {
            throw new RuntimeException();
        }
    }

    private static class BooleanExitAdvice {

        @Advice.OnMethodExit
        public static boolean foo() {
            throw new RuntimeException();
        }
    }

    private static class ByteExitAdvice {

        @Advice.OnMethodExit
        public static byte foo() {
            throw new RuntimeException();
        }
    }

    private static class ShortExitAdvice {

        @Advice.OnMethodExit
        public static short foo() {
            throw new RuntimeException();
        }
    }

    private static class CharacterExitAdvice {

        @Advice.OnMethodExit
        public static char foo() {
            throw new RuntimeException();
        }
    }

    private static class IntegerExitAdvice {

        @Advice.OnMethodExit
        public static int foo() {
            throw new RuntimeException();
        }
    }

    private static class LongExitAdvice {

        @Advice.OnMethodExit
        public static long foo() {
            throw new RuntimeException();
        }
    }

    private static class FloatExitAdvice {

        @Advice.OnMethodExit
        public static float foo() {
            throw new RuntimeException();
        }
    }

    private static class DoubleExitAdvice {

        @Advice.OnMethodExit
        public static double foo() {
            throw new RuntimeException();
        }
    }

    private static class ReferenceExitAdvice {

        @Advice.OnMethodExit
        public static Object foo() {
            throw new RuntimeException();
        }
    }

    private static class VoidExitHandlerAdvice {

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        public static void foo() {
            throw new RuntimeException();
        }
    }

    private static class BooleanExitHandlerAdvice {

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        public static boolean foo() {
            throw new RuntimeException();
        }
    }

    private static class ByteExitHandlerAdvice {

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        public static byte foo() {
            throw new RuntimeException();
        }
    }

    private static class ShortExitHandlerAdvice {

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        public static short foo() {
            throw new RuntimeException();
        }
    }

    private static class CharacterExitHandlerAdvice {

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        public static char foo() {
            throw new RuntimeException();
        }
    }

    private static class IntegerExitHandlerAdvice {

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        public static int foo() {
            throw new RuntimeException();
        }
    }

    private static class LongExitHandlerAdvice {

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        public static long foo() {
            throw new RuntimeException();
        }
    }

    private static class FloatExitHandlerAdvice {

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        public static float foo() {
            throw new RuntimeException();
        }
    }

    private static class DoubleExitHandlerAdvice {

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        public static double foo() {
            throw new RuntimeException();
        }
    }

    private static class ReferenceExitHandlerAdvice {

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        public static Object foo() {
            throw new RuntimeException();
        }
    }
}
