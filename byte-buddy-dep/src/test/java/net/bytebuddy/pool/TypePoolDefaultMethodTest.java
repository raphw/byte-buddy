package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isConstructor;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isMethod;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class TypePoolDefaultMethodTest {

    private static final String METHOD = "method", ARRAY_METHOD = "array", NESTED_ARRAY = "nestedArray";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BooleanSample.class, boolean.class},
                {ByteSample.class, byte.class},
                {ShortSample.class, short.class},
                {CharacterSample.class, char.class},
                {IntegerSample.class, int.class},
                {LongSample.class, long.class},
                {FloatSample.class, float.class},
                {DoubleSample.class, double.class},
                {ReferenceSample.class, Object.class}
        });
    }

    private final Class<?> sample, type;

    public TypePoolDefaultMethodTest(Class<?> sample, Class<?> type) {
        this.sample = sample;
        this.type = type;
    }

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @Test
    public void testMethodExtraction() throws Exception {
        TypeDescription typeDescription = typePool.describe(sample.getName());
        assertThat(typeDescription.getDeclaredMethods().filter(isMethod()).size(), is(3));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().isConstructor(), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().represents(sample.getDeclaredConstructor()), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(METHOD)).getOnly().getReturnType().represents(type), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(METHOD)).getOnly().getParameterTypes().get(0).represents(type), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(METHOD)).getOnly().getDeclaringType(), is(typeDescription));
        assertThat(typeDescription.getDeclaredMethods().filter(named(METHOD)).getOnly().getModifiers(), is(Opcodes.ACC_ABSTRACT));
        assertThat(typeDescription.getDeclaredMethods().filter(named(METHOD)).getOnly().isMethod(), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(ARRAY_METHOD)).getOnly().getReturnType().isArray(), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(ARRAY_METHOD)).getOnly().getReturnType().getComponentType().represents(type), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(ARRAY_METHOD)).getOnly().getParameterTypes().get(0).isArray(), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(ARRAY_METHOD)).getOnly().getParameterTypes().get(0).getComponentType().represents(type), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(NESTED_ARRAY)).getOnly().getReturnType().isArray(), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(NESTED_ARRAY)).getOnly().getReturnType().getComponentType().isArray(), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(NESTED_ARRAY)).getOnly().getReturnType().getComponentType().getComponentType().represents(type), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(NESTED_ARRAY)).getOnly().getParameterTypes().get(0).isArray(), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(NESTED_ARRAY)).getOnly().getParameterTypes().get(0).getComponentType().isArray(), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(NESTED_ARRAY)).getOnly().getParameterTypes().get(0).getComponentType().getComponentType().represents(type), is(true));
    }

    @SuppressWarnings("unused")
    private abstract static class BooleanSample {

        abstract boolean method(boolean b);

        abstract boolean[] array(boolean[] b);

        abstract boolean[][] nestedArray(boolean[][] b);
    }

    @SuppressWarnings("unused")
    private abstract static class ByteSample {

        abstract byte method(byte b);

        abstract byte[] array(byte[] b);

        abstract byte[][] nestedArray(byte[][] b);
    }

    @SuppressWarnings("unused")
    private abstract static class ShortSample {

        abstract short method(short b);

        abstract short[] array(short[] b);

        abstract short[][] nestedArray(short[][] b);
    }

    @SuppressWarnings("unused")
    private abstract static class CharacterSample {

        abstract char method(char b);

        abstract char[] array(char[] b);

        abstract char[][] nestedArray(char[][] b);
    }

    @SuppressWarnings("unused")
    private abstract static class IntegerSample {

        abstract int method(int b);

        abstract int[] array(int[] b);

        abstract int[][] nestedArray(int[][] b);
    }

    @SuppressWarnings("unused")
    private abstract static class LongSample {

        abstract long method(long b);

        abstract long[] array(long[] b);

        abstract long[][] nestedArray(long[][] b);
    }

    @SuppressWarnings("unused")
    private abstract static class FloatSample {

        abstract float method(float b);

        abstract float[] array(float[] b);

        abstract float[][] nestedArray(float[][] b);
    }

    @SuppressWarnings("unused")
    private abstract static class DoubleSample {

        abstract double method(double b);

        abstract double[] array(double[] b);

        abstract double[][] nestedArray(double[][] b);
    }

    @SuppressWarnings("unused")
    private abstract static class ReferenceSample {

        abstract Object method(Object b);

        abstract Object[] array(Object[] b);

        abstract Object[][] nestedArray(Object[][] b);
    }
}
