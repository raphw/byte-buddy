package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class TypePoolDefaultFieldTest {

    private static final String FIELD = "field", ARRAY_FIELD = "array", NESTED_ARRAY = "nestedArray";

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

    public TypePoolDefaultFieldTest(Class<?> sample, Class<?> type) {
        this.sample = sample;
        this.type = type;
    }

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @Test
    public void testFieldExtraction() throws Exception {
        TypeDescription typeDescription = typePool.describe(sample.getName());
        assertThat(typeDescription.getDeclaredFields().size(), is(3));
        assertThat(typeDescription.getDeclaredFields().named(FIELD).getFieldType().represents(type), is(true));
        assertThat(typeDescription.getDeclaredFields().named(FIELD).getDeclaringType(), is(typeDescription));
        assertThat(typeDescription.getDeclaredFields().named(FIELD).getModifiers(), is(Opcodes.ACC_PRIVATE));
        assertThat(typeDescription.getDeclaredFields().named(ARRAY_FIELD).getFieldType().isArray(), is(true));
        assertThat(typeDescription.getDeclaredFields().named(ARRAY_FIELD).getFieldType().getComponentType().represents(type), is(true));
        assertThat(typeDescription.getDeclaredFields().named(NESTED_ARRAY).getFieldType().isArray(), is(true));
        assertThat(typeDescription.getDeclaredFields().named(NESTED_ARRAY).getFieldType().getComponentType().isArray(), is(true));
        assertThat(typeDescription.getDeclaredFields().named(NESTED_ARRAY).getFieldType().getComponentType().getComponentType().represents(type), is(true));
    }

    @SuppressWarnings("unused")
    private static class BooleanSample {

        private boolean field;

        private boolean[] array;

        private boolean[][] nestedArray;
    }

    @SuppressWarnings("unused")
    private static class ByteSample {

        private byte field;

        private byte[] array;

        private byte[][] nestedArray;
    }

    @SuppressWarnings("unused")
    private static class ShortSample {

        private short field;

        private short[] array;

        private short[][] nestedArray;
    }

    @SuppressWarnings("unused")
    private static class CharacterSample {

        private char field;

        private char[] array;

        private char[][] nestedArray;
    }

    @SuppressWarnings("unused")
    private static class IntegerSample {

        private int field;

        private int[] array;

        private int[][] nestedArray;
    }

    @SuppressWarnings("unused")
    private static class LongSample {

        private long field;

        private long[] array;

        private long[][] nestedArray;
    }

    @SuppressWarnings("unused")
    private static class FloatSample {

        private float field;

        private float[] array;

        private float[][] nestedArray;
    }

    @SuppressWarnings("unused")
    private static class DoubleSample {

        private double field;

        private double[] array;

        private double[][] nestedArray;
    }

    @SuppressWarnings("unused")
    private static class ReferenceSample {

        private Object field;

        private Object[] array;

        private Object[][] nestedArray;
    }
}
