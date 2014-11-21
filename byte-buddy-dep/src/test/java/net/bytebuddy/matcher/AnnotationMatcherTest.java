package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotatedElement;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AnnotationMatcherTest extends AbstractElementMatcherTest<AnnotationMatcher<?>> {

    @Mock
    private ElementMatcher<List<? extends AnnotationDescription>> annotationTypeMatcher;

    @Mock
    private AnnotatedElement annotatedElement;

    @Mock
    private AnnotationList annotationList;

    @SuppressWarnings("unchecked")
    public AnnotationMatcherTest() {
        super((Class<AnnotationMatcher<?>>) (Object) AnnotationMatcher.class, "hasAnnotation");
    }

    @Before
    public void setUp() throws Exception {
        when(annotatedElement.getDeclaredAnnotations()).thenReturn(annotationList);
    }

    @Test
    public void testMatch() throws Exception {
        when(annotationTypeMatcher.matches(annotationList)).thenReturn(true);
        assertThat(new AnnotationMatcher<AnnotatedElement>(annotationTypeMatcher).matches(annotatedElement), is(true));
        verify(annotationTypeMatcher).matches(annotationList);
        verifyNoMoreInteractions(annotationTypeMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(annotationTypeMatcher.matches(annotationList)).thenReturn(false);
        assertThat(new AnnotationMatcher<AnnotatedElement>(annotationTypeMatcher).matches(annotatedElement), is(false));
        verify(annotationTypeMatcher).matches(annotationList);
        verifyNoMoreInteractions(annotationTypeMatcher);
    }
}
