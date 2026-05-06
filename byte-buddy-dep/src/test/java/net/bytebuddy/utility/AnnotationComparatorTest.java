package net.bytebuddy.utility;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.lang.annotation.Annotation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class AnnotationComparatorTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Annotation left, right;

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testTypeComparison() {
        when(left.annotationType()).thenReturn((Class) Object.class);
        when(right.annotationType()).thenReturn((Class) String.class);
        assertThat(AnnotationComparator.INSTANCE.compare(left, left) == 0, is(true));
        assertThat(AnnotationComparator.INSTANCE.compare(left, right) < 0, is(true));
        assertThat(AnnotationComparator.INSTANCE.compare(right, left) > 0, is(true));
    }

    @Test
    public void testNullSafe() {
        assertThat(AnnotationComparator.INSTANCE.compare(null, null) == 0, is(true));
        assertThat(AnnotationComparator.INSTANCE.compare(null, right) > 0, is(true));
        assertThat(AnnotationComparator.INSTANCE.compare(left, null) < 0, is(true));
    }
}
