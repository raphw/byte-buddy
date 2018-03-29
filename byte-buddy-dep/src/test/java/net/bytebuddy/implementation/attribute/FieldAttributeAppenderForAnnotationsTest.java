package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationList;
import org.junit.Test;
import org.objectweb.asm.Type;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class FieldAttributeAppenderForAnnotationsTest extends AbstractFieldAttributeAppenderTest {

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new FieldAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new Qux.Instance())).apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verifyZeroInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new FieldAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new Baz.Instance())).apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new FieldAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance())).apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testFactory() throws Exception {
        FieldAttributeAppender.Explicit fieldAttributeAppender = new FieldAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        assertThat(fieldAttributeAppender.make(instrumentedType), sameInstance((FieldAttributeAppender) fieldAttributeAppender));
    }

    public @interface SimpleAnnotation {

        String value();
    }
}
