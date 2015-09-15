package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AnnotationAppenderValueFilterSkipDefaultsTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private AnnotationDescription annotationDescription;

    @Mock
    private MethodDescription.InDefinedShape methodDescription;

    @Test
    public void testFilteringNoDefault() throws Exception {
        assertThat(AnnotationAppender.ValueFilter.SkipDefaults.INSTANCE.isRelevant(annotationDescription, methodDescription), is(false));
        verifyZeroInteractions(annotationDescription);
        verify(methodDescription).getDefaultValue();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testFilteringNoEquality() throws Exception {
        when(methodDescription.getDefaultValue()).thenReturn(new Object());
        when(annotationDescription.getValue(methodDescription)).thenReturn(new Object());
        assertThat(AnnotationAppender.ValueFilter.SkipDefaults.INSTANCE.isRelevant(annotationDescription, methodDescription), is(false));
        verify(annotationDescription).getValue(methodDescription);
        verifyNoMoreInteractions(annotationDescription);
        verify(methodDescription).getDefaultValue();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testFilteringEquality() throws Exception {
        Object value = new Object();
        when(methodDescription.getDefaultValue()).thenReturn(value);
        when(annotationDescription.getValue(methodDescription)).thenReturn(value);
        assertThat(AnnotationAppender.ValueFilter.SkipDefaults.INSTANCE.isRelevant(annotationDescription, methodDescription), is(true));
        verify(annotationDescription).getValue(methodDescription);
        verifyNoMoreInteractions(annotationDescription);
        verify(methodDescription).getDefaultValue();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AnnotationAppender.ValueFilter.SkipDefaults.class).apply();
    }
}