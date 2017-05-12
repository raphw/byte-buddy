package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;

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
        when(fieldDescription.getType()).thenReturn(TypeDescription.Generic.OBJECT);
        when(fieldDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        FieldAttributeAppender.ForInstrumentedField.INSTANCE.apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verifyZeroInteractions(fieldVisitor);
        verify(fieldDescription).getDeclaredAnnotations();
        verify(fieldDescription).getType();
        verifyNoMoreInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        when(fieldDescription.getType()).thenReturn(TypeDescription.Generic.OBJECT);
        when(fieldDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        FieldAttributeAppender.ForInstrumentedField.INSTANCE.apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(fieldVisitor);
        verify(fieldDescription).getDeclaredAnnotations();
        verify(fieldDescription).getType();
        verifyNoMoreInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        when(fieldDescription.getType()).thenReturn(TypeDescription.Generic.OBJECT);
        when(fieldDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        FieldAttributeAppender.ForInstrumentedField.INSTANCE.apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(fieldVisitor);
        verify(fieldDescription).getDeclaredAnnotations();
        verify(fieldDescription).getType();
        verifyNoMoreInteractions(fieldDescription);
    }

    @Test
    public void testFieldTypeTypeAnnotationNoRetention() throws Exception {
        when(fieldDescription.getType()).thenReturn(simpleAnnotatedType);
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(fieldDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        FieldAttributeAppender.ForInstrumentedField.INSTANCE.apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verifyZeroInteractions(fieldVisitor);
        verify(fieldDescription).getDeclaredAnnotations();
        verify(fieldDescription).getType();
        verifyNoMoreInteractions(fieldDescription);
    }

    @Test
    public void testFieldTypeTypeAnnotationRuntimeRetention() throws Exception {
        when(fieldDescription.getType()).thenReturn(simpleAnnotatedType);
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(fieldDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        FieldAttributeAppender.ForInstrumentedField.INSTANCE.apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verify(fieldVisitor).visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.FIELD).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verifyNoMoreInteractions(fieldVisitor);
        verify(fieldDescription).getDeclaredAnnotations();
        verify(fieldDescription).getType();
        verifyNoMoreInteractions(fieldDescription);
    }

    @Test
    public void testFieldTypeTypeAnnotationByteCodeRetention() throws Exception {
        when(fieldDescription.getType()).thenReturn(simpleAnnotatedType);
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(fieldDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        FieldAttributeAppender.ForInstrumentedField.INSTANCE.apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verify(fieldVisitor).visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.FIELD).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verifyNoMoreInteractions(fieldVisitor);
        verify(fieldDescription).getDeclaredAnnotations();
        verify(fieldDescription).getType();
        verifyNoMoreInteractions(fieldDescription);
    }
}
