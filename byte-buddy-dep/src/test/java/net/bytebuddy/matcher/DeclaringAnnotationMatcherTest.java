package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotatedElement;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DeclaringAnnotationMatcherTest extends AbstractElementMatcherTest<DeclaringAnnotationMatcher<?>> {

    @Mock
    private ElementMatcher<? super AnnotationList> annotationMatcher;

    @Mock
    private AnnotatedElement annotatedElement;

    @Mock
    private AnnotationList annotationList;

    @SuppressWarnings("unchecked")
    public DeclaringAnnotationMatcherTest() {
        super((Class<DeclaringAnnotationMatcher<?>>) (Object) DeclaringAnnotationMatcher.class, "declaresAnnotations");
    }

    @Test
    public void testMatch() throws Exception {
        when(annotatedElement.getDeclaredAnnotations()).thenReturn(annotationList);
        when(annotationMatcher.matches(annotationList)).thenReturn(true);
        assertThat(new DeclaringAnnotationMatcher<AnnotatedElement>(annotationMatcher).matches(annotatedElement), is(true));
        verify(annotationMatcher).matches(annotationList);
        verifyNoMoreInteractions(annotationMatcher);
        verify(annotatedElement).getDeclaredAnnotations();
        verifyNoMoreInteractions(annotatedElement);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(annotatedElement.getDeclaredAnnotations()).thenReturn(annotationList);
        when(annotationMatcher.matches(annotationList)).thenReturn(false);
        assertThat(new DeclaringAnnotationMatcher<AnnotatedElement>(annotationMatcher).matches(annotatedElement), is(false));
        verify(annotationMatcher).matches(annotationList);
        verifyNoMoreInteractions(annotationMatcher);
        verify(annotatedElement).getDeclaredAnnotations();
        verifyNoMoreInteractions(annotatedElement);
    }
}
