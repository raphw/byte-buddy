package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AnnotationValueFilterDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private AnnotationDescription annotationDescription;

    @Mock
    private MethodDescription.InDefinedShape methodDescription;

    @Test
    public void testAppendsDefaults() throws Exception {
        AnnotationDescription annotationDescription = mock(AnnotationDescription.class);
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        assertThat(AnnotationValueFilter.Default.APPEND_DEFAULTS.isRelevant(annotationDescription, methodDescription), is(true));
        verifyZeroInteractions(annotationDescription);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    public void testSkipDefaultsNoDefault() throws Exception {
        assertThat(AnnotationValueFilter.Default.SKIP_DEFAULTS.isRelevant(annotationDescription, methodDescription), is(true));
        verifyZeroInteractions(annotationDescription);
        verify(methodDescription).getDefaultValue();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testSkipDefaultsNoEquality() throws Exception {
        when(methodDescription.getDefaultValue()).thenReturn(new Object());
        when(annotationDescription.getValue(methodDescription)).thenReturn(new Object());
        assertThat(AnnotationValueFilter.Default.SKIP_DEFAULTS.isRelevant(annotationDescription, methodDescription), is(true));
        verify(annotationDescription).getValue(methodDescription);
        verifyNoMoreInteractions(annotationDescription);
        verify(methodDescription).getDefaultValue();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testSkipDefaultsEquality() throws Exception {
        Object value = new Object();
        when(methodDescription.getDefaultValue()).thenReturn(value);
        when(annotationDescription.getValue(methodDescription)).thenReturn(value);
        assertThat(AnnotationValueFilter.Default.SKIP_DEFAULTS.isRelevant(annotationDescription, methodDescription), is(false));
        verify(annotationDescription).getValue(methodDescription);
        verifyNoMoreInteractions(annotationDescription);
        verify(methodDescription).getDefaultValue();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testFactory() throws Exception {
        assertThat(AnnotationValueFilter.Default.SKIP_DEFAULTS.on(mock(FieldDescription.class)),
                is((AnnotationValueFilter) AnnotationValueFilter.Default.SKIP_DEFAULTS));
        assertThat(AnnotationValueFilter.Default.SKIP_DEFAULTS.on(mock(MethodDescription.class)),
                is((AnnotationValueFilter) AnnotationValueFilter.Default.SKIP_DEFAULTS));
        assertThat(AnnotationValueFilter.Default.SKIP_DEFAULTS.on(mock(TypeDescription.class)),
                is((AnnotationValueFilter) AnnotationValueFilter.Default.SKIP_DEFAULTS));
        assertThat(AnnotationValueFilter.Default.APPEND_DEFAULTS.on(mock(FieldDescription.class)),
                is((AnnotationValueFilter) AnnotationValueFilter.Default.APPEND_DEFAULTS));
        assertThat(AnnotationValueFilter.Default.APPEND_DEFAULTS.on(mock(MethodDescription.class)),
                is((AnnotationValueFilter) AnnotationValueFilter.Default.APPEND_DEFAULTS));
        assertThat(AnnotationValueFilter.Default.APPEND_DEFAULTS.on(mock(TypeDescription.class)),
                is((AnnotationValueFilter) AnnotationValueFilter.Default.APPEND_DEFAULTS));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AnnotationAppender.Default.class).apply();
    }
}