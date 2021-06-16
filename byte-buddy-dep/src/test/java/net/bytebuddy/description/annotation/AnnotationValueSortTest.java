package net.bytebuddy.description.annotation;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AnnotationValueSortTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {AnnotationValue.Sort.BOOLEAN, boolean.class, 'Z', true},
                {AnnotationValue.Sort.BYTE, byte.class, 'B', true},
                {AnnotationValue.Sort.SHORT, short.class, 'S', true},
                {AnnotationValue.Sort.CHARACTER, char.class, 'C', true},
                {AnnotationValue.Sort.INTEGER, int.class, 'I', true},
                {AnnotationValue.Sort.LONG, long.class, 'J', true},
                {AnnotationValue.Sort.FLOAT, float.class, 'F', true},
                {AnnotationValue.Sort.DOUBLE, double.class, 'D', true},
                {AnnotationValue.Sort.STRING, String.class, 's', true},
                {AnnotationValue.Sort.TYPE, Class.class, 'c', true},
                {AnnotationValue.Sort.ENUMERATION, SampleEnumeration.class, 'e', true},
                {AnnotationValue.Sort.ANNOTATION, SampleAnnotation.class, '@', true},
                {AnnotationValue.Sort.ARRAY, Object[].class, '[', true},
                {AnnotationValue.Sort.NONE, Object.class, 0, false}
        });
    }

    private final AnnotationValue.Sort sort;

    private final Class<?> type;

    private final int tag;

    private final boolean defined;

    public AnnotationValueSortTest(AnnotationValue.Sort sort, Class<?> type, int tag, boolean defined) {
        this.sort = sort;
        this.type = type;
        this.tag = tag;
        this.defined = defined;
    }

    @Test
    public void testSortResolution() {
        assertThat(AnnotationValue.Sort.of(TypeDescription.ForLoadedType.of(type)), is(sort));
    }

    @Test
    public void testTag() {
        assertThat(sort.getTag(), is(tag));
    }

    @Test
    public void testDefined() {
        assertThat(sort.isDefined(), is(defined));
    }

    private enum SampleEnumeration {
        FOO
    }

    private @interface SampleAnnotation {
        /* empty */
    }
}
