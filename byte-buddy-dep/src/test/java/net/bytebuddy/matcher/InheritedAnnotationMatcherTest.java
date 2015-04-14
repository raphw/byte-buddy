package net.bytebuddy.matcher;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class InheritedAnnotationMatcherTest extends AbstractElementMatcherTest<InheritedAnnotationMatcher<?>> {

    @Mock
    private ElementMatcher<? super AnnotationList> annotationMatcher;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private AnnotationList annotationList;

    @SuppressWarnings("unchecked")
    public InheritedAnnotationMatcherTest() {
        super((Class<InheritedAnnotationMatcher<?>>) (Object) InheritedAnnotationMatcher.class, "inheritsAnnotations");
    }

    @Test
    public void testMatch() throws Exception {
        when(typeDescription.getInheritedAnnotations()).thenReturn(annotationList);
        when(annotationMatcher.matches(annotationList)).thenReturn(true);
        assertThat(new InheritedAnnotationMatcher<TypeDescription>(annotationMatcher).matches(typeDescription), is(true));
        verify(annotationMatcher).matches(annotationList);
        verifyNoMoreInteractions(annotationMatcher);
        verify(typeDescription).getInheritedAnnotations();
        verifyNoMoreInteractions(typeDescription);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(typeDescription.getInheritedAnnotations()).thenReturn(annotationList);
        when(annotationMatcher.matches(annotationList)).thenReturn(false);
        assertThat(new InheritedAnnotationMatcher<TypeDescription>(annotationMatcher).matches(typeDescription), is(false));
        verify(annotationMatcher).matches(annotationList);
        verifyNoMoreInteractions(annotationMatcher);
        verify(typeDescription).getInheritedAnnotations();
        verifyNoMoreInteractions(typeDescription);
    }
}
