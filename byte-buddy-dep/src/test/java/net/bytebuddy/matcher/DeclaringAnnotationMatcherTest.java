package net.bytebuddy.matcher;

import net.bytebuddy.description.annotation.AnnotatedCodeElement;
import net.bytebuddy.description.annotation.AnnotationList;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DeclaringAnnotationMatcherTest extends AbstractElementMatcherTest<DeclaringAnnotationMatcher<?>> {

    @Mock
    private ElementMatcher<? super AnnotationList> annotationMatcher;

    @Mock
    private AnnotatedCodeElement annotatedCodeElement;

    @Mock
    private AnnotationList annotationList;

    @SuppressWarnings("unchecked")
    public DeclaringAnnotationMatcherTest() {
        super((Class<DeclaringAnnotationMatcher<?>>) (Object) DeclaringAnnotationMatcher.class, "declaresAnnotations");
    }

    @Test
    public void testMatch() throws Exception {
        when(annotatedCodeElement.getDeclaredAnnotations()).thenReturn(annotationList);
        when(annotationMatcher.matches(annotationList)).thenReturn(true);
        assertThat(new DeclaringAnnotationMatcher<AnnotatedCodeElement>(annotationMatcher).matches(annotatedCodeElement), is(true));
        verify(annotationMatcher).matches(annotationList);
        verifyNoMoreInteractions(annotationMatcher);
        verify(annotatedCodeElement).getDeclaredAnnotations();
        verifyNoMoreInteractions(annotatedCodeElement);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(annotatedCodeElement.getDeclaredAnnotations()).thenReturn(annotationList);
        when(annotationMatcher.matches(annotationList)).thenReturn(false);
        assertThat(new DeclaringAnnotationMatcher<AnnotatedCodeElement>(annotationMatcher).matches(annotatedCodeElement), is(false));
        verify(annotationMatcher).matches(annotationList);
        verifyNoMoreInteractions(annotationMatcher);
        verify(annotatedCodeElement).getDeclaredAnnotations();
        verifyNoMoreInteractions(annotatedCodeElement);
    }
}
