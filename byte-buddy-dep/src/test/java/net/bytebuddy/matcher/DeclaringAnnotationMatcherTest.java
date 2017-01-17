package net.bytebuddy.matcher;

import net.bytebuddy.description.annotation.AnnotationSource;
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
    private AnnotationSource annotationSource;

    @Mock
    private AnnotationList annotationList;

    @SuppressWarnings("unchecked")
    public DeclaringAnnotationMatcherTest() {
        super((Class<DeclaringAnnotationMatcher<?>>) (Object) DeclaringAnnotationMatcher.class, "declaresAnnotations");
    }

    @Test
    public void testMatch() throws Exception {
        when(annotationSource.getDeclaredAnnotations()).thenReturn(annotationList);
        when(annotationMatcher.matches(annotationList)).thenReturn(true);
        assertThat(new DeclaringAnnotationMatcher<AnnotationSource>(annotationMatcher).matches(annotationSource), is(true));
        verify(annotationMatcher).matches(annotationList);
        verifyNoMoreInteractions(annotationMatcher);
        verify(annotationSource).getDeclaredAnnotations();
        verifyNoMoreInteractions(annotationSource);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(annotationSource.getDeclaredAnnotations()).thenReturn(annotationList);
        when(annotationMatcher.matches(annotationList)).thenReturn(false);
        assertThat(new DeclaringAnnotationMatcher<AnnotationSource>(annotationMatcher).matches(annotationSource), is(false));
        verify(annotationMatcher).matches(annotationList);
        verifyNoMoreInteractions(annotationMatcher);
        verify(annotationSource).getDeclaredAnnotations();
        verifyNoMoreInteractions(annotationSource);
    }
}
