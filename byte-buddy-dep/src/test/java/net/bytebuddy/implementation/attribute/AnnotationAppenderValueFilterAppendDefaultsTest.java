package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class AnnotationAppenderValueFilterAppendDefaultsTest {

    @Test
    public void testFiltering() throws Exception {
        AnnotationDescription annotationDescription = mock(AnnotationDescription.class);
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        assertThat(AnnotationAppender.ValueFilter.AppendDefaults.INSTANCE.isRelevant(annotationDescription, methodDescription), is(true));
        verifyZeroInteractions(annotationDescription);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AnnotationAppender.ValueFilter.AppendDefaults.class).apply();
    }
}