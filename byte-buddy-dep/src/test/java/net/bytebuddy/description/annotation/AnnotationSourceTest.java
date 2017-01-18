package net.bytebuddy.description.annotation;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AnnotationSourceTest {

    @Test
    public void testEmpty() throws Exception {
        assertThat(AnnotationSource.Empty.INSTANCE.getDeclaredAnnotations().size(), is(0));
    }

    @Test
    public void testExplicit() throws Exception {
        AnnotationDescription annotationDescription = mock(AnnotationDescription.class);
        assertThat(new AnnotationSource.Explicit(annotationDescription).getDeclaredAnnotations().size(), is(1));
        assertThat(new AnnotationSource.Explicit(annotationDescription).getDeclaredAnnotations().getOnly(), is(annotationDescription));
    }
}
