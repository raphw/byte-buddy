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
public class AdviceSizeConversionTest {

    private static final String FOO = "foo";

    private static final int NUMERIC = 42;

    private final Class<?> target, parameter;

    private final Object input, output;

    public AdviceSizeConversionTest(Class<?> target, Class<?> parameter, Object input, Object output) {
        this.target = target;
        this.parameter = parameter;
        this.input = input;
        this.output = output;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {IntToFloat.class, int.class, NUMERIC, (float) NUMERIC},
                {IntToLong.class, int.class, NUMERIC, (long) NUMERIC},
                {IntToDouble.class, int.class, NUMERIC, (double) NUMERIC},
                {FloatToInt.class, float.class, (float) NUMERIC, NUMERIC},
                {FloatToLong.class, float.class, (float) NUMERIC, (long) NUMERIC},
                {FloatToDouble.class, float.class, (float) NUMERIC, (double) NUMERIC},
                {LongToInt.class, long.class, (long) NUMERIC, NUMERIC},
                {LongToFloat.class, long.class, (long) NUMERIC, (float) NUMERIC},
                {LongToDouble.class, long.class, (long) NUMERIC, (double) NUMERIC},
                {DoubleToInt.class, double.class, (double) NUMERIC, NUMERIC},
                {DoubleToLong.class, double.class, (double) NUMERIC, (long) NUMERIC},
                {DoubleToFloat.class, double.class, (double) NUMERIC, (float) NUMERIC},
        });
    }

    @Test
    public void testAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(target)
                .visit(Advice.to(AdviceSizeConversionTest.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, parameter).invoke(type.getDeclaredConstructor().newInstance(), input), is(output));
    }

    @Advice.OnMethodExit
    private static void exit() {
        /* empty */
    }

    public static class IntToFloat {

        public float foo(int value) {
            return (float) value;
        }
    }

    public static class IntToLong {

        public long foo(int value) {
            return (long) value;
        }
    }

    public static class IntToDouble {

        public double foo(int value) {
            return (double) value;
        }
    }

    public static class FloatToInt {

        public int foo(float value) {
            return (int) value;
        }
    }

    public static class FloatToLong {

        public long foo(float value) {
            return (long) value;
        }
    }

    public static class FloatToDouble {

        public double foo(float value) {
            return (double) value;
        }
    }

    public static class LongToInt {

        public int foo(long value) {
            return (int) value;
        }
    }

    public static class LongToFloat {

        public float foo(long value) {
            return (float) value;
        }
    }

    public static class LongToDouble {

        public double foo(long value) {
            return (double) value;
        }
    }

    public static class DoubleToInt {

        public int foo(double value) {
            return (int) value;
        }
    }

    public static class DoubleToFloat {

        public float foo(double value) {
            return (float) value;
        }
    }

    public static class DoubleToLong {

        public long foo(double value) {
            return (long) value;
        }
    }

}
