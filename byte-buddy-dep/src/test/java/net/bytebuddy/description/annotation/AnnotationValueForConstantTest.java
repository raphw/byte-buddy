package net.bytebuddy.description.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AnnotationValueForConstantTest {

    private static final String FOO = "foo";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {false, AnnotationValue.Sort.BOOLEAN},
                {(byte) 0, AnnotationValue.Sort.BYTE},
                {(short) 0, AnnotationValue.Sort.SHORT},
                {(char) 0, AnnotationValue.Sort.CHARACTER},
                {0, AnnotationValue.Sort.INTEGER},
                {0L, AnnotationValue.Sort.LONG},
                {0f, AnnotationValue.Sort.FLOAT},
                {0d, AnnotationValue.Sort.DOUBLE},
                {FOO, AnnotationValue.Sort.STRING},
                {new boolean[]{false}, AnnotationValue.Sort.ARRAY},
                {new byte[]{0}, AnnotationValue.Sort.ARRAY},
                {new short[]{0}, AnnotationValue.Sort.ARRAY},
                {new char[]{0}, AnnotationValue.Sort.ARRAY},
                {new int[]{0}, AnnotationValue.Sort.ARRAY},
                {new long[]{0L}, AnnotationValue.Sort.ARRAY},
                {new float[]{0f}, AnnotationValue.Sort.ARRAY},
                {new double[]{0d}, AnnotationValue.Sort.ARRAY},
                {new String[]{FOO}, AnnotationValue.Sort.ARRAY}
        });
    }

    private final Object value;

    private final AnnotationValue.Sort sort;

    public AnnotationValueForConstantTest(Object value, AnnotationValue.Sort sort) {
        this.value = value;
        this.sort = sort;
    }

    @Test
    public void testValue() {
        assertThat(AnnotationValue.ForConstant.of(value).resolve(), is(value));
    }

    @Test
    public void testValueLoaded() {
        assertThat(AnnotationValue.ForConstant.of(value).load(AnnotationValueForConstantTest.class.getClassLoader()).resolve(), is(value));
    }

    @Test
    public void testSort() {
        assertThat(AnnotationValue.ForConstant.of(value).getSort(), is(sort));
    }
}
