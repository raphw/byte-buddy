package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationList;
import org.junit.Test;
import org.mockito.asm.Type;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class FieldAttributeAppenderForInstrumentedFieldTest extends AbstractFieldAttributeAppenderTest {

    @Test
    public void testFactory() throws Exception {
        assertThat(FieldAttributeAppender.ForInstrumentedField.INSTANCE.make(instrumentedType),
                is((FieldAttributeAppender) FieldAttributeAppender.ForInstrumentedField.INSTANCE));
    }

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        when(fieldDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotation(new Qux.Instance()));
        FieldAttributeAppender.ForInstrumentedField.INSTANCE.apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verifyZeroInteractions(fieldVisitor);
        verify(fieldDescription).getDeclaredAnnotations();
        verifyNoMoreInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        when(fieldDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotation(new Baz.Instance()));
        FieldAttributeAppender.ForInstrumentedField.INSTANCE.apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(fieldVisitor);
        verify(fieldDescription).getDeclaredAnnotations();
        verifyNoMoreInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        when(fieldDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotation(new QuxBaz.Instance()));
        FieldAttributeAppender.ForInstrumentedField.INSTANCE.apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(fieldVisitor);
        verify(fieldDescription).getDeclaredAnnotations();
        verifyNoMoreInteractions(fieldDescription);
    }
}
