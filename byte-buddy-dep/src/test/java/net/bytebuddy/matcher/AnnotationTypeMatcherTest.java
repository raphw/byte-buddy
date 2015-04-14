package net.bytebuddy.matcher;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AnnotationTypeMatcherTest extends AbstractElementMatcherTest<AnnotationTypeMatcher<?>> {

    @Mock
    private ElementMatcher<? super TypeDescription> typeMatcher;

    @Mock
    private AnnotationDescription annotatedElement;

    @Mock
    private TypeDescription annotationType;

    @SuppressWarnings("unchecked")
    public AnnotationTypeMatcherTest() {
        super((Class<AnnotationTypeMatcher<?>>) (Object) AnnotationTypeMatcher.class, "ofAnnotationType");
    }

    @Before
    public void setUp() throws Exception {
        when(annotatedElement.getAnnotationType()).thenReturn(annotationType);
    }

    @Test
    public void testMatch() throws Exception {
        when(typeMatcher.matches(annotationType)).thenReturn(true);
        assertThat(new AnnotationTypeMatcher<AnnotationDescription>(typeMatcher).matches(annotatedElement), is(true));
        verify(typeMatcher).matches(annotationType);
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(typeMatcher.matches(annotationType)).thenReturn(false);
        assertThat(new AnnotationTypeMatcher<AnnotationDescription>(typeMatcher).matches(annotatedElement), is(false));
        verify(typeMatcher).matches(annotationType);
        verifyNoMoreInteractions(typeMatcher);
    }
}
