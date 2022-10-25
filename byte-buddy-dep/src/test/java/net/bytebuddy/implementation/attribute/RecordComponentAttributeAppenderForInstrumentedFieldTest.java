package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class RecordComponentAttributeAppenderForInstrumentedFieldTest extends AbstractRecordComponentAttributeAppenderTest {

    @Test
    public void testFactory() throws Exception {
        assertThat(RecordComponentAttributeAppender.ForInstrumentedRecordComponent.INSTANCE.make(instrumentedType),
                is((RecordComponentAttributeAppender) RecordComponentAttributeAppender.ForInstrumentedRecordComponent.INSTANCE));
    }

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        when(recordComponentDescription.getType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class));
        when(recordComponentDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        RecordComponentAttributeAppender.ForInstrumentedRecordComponent.INSTANCE.apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verifyNoMoreInteractions(recordComponentVisitor);
        verify(recordComponentDescription).getDeclaredAnnotations();
        verify(recordComponentDescription).getType();
        verifyNoMoreInteractions(recordComponentDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        when(recordComponentDescription.getType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class));
        when(recordComponentDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        RecordComponentAttributeAppender.ForInstrumentedRecordComponent.INSTANCE.apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verify(recordComponentVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(recordComponentVisitor);
        verify(recordComponentDescription).getDeclaredAnnotations();
        verify(recordComponentDescription).getType();
        verifyNoMoreInteractions(recordComponentDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        when(recordComponentDescription.getType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class));
        when(recordComponentDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        RecordComponentAttributeAppender.ForInstrumentedRecordComponent.INSTANCE.apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verify(recordComponentVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(recordComponentVisitor);
        verify(recordComponentDescription).getDeclaredAnnotations();
        verify(recordComponentDescription).getType();
        verifyNoMoreInteractions(recordComponentDescription);
    }

    @Test
    public void testFieldTypeTypeAnnotationNoRetention() throws Exception {
        when(recordComponentDescription.getType()).thenReturn(simpleAnnotatedType);
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(recordComponentDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        RecordComponentAttributeAppender.ForInstrumentedRecordComponent.INSTANCE.apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verifyNoMoreInteractions(recordComponentVisitor);
        verify(recordComponentDescription).getDeclaredAnnotations();
        verify(recordComponentDescription).getType();
        verifyNoMoreInteractions(recordComponentDescription);
    }

    @Test
    public void testFieldTypeTypeAnnotationRuntimeRetention() throws Exception {
        when(recordComponentDescription.getType()).thenReturn(simpleAnnotatedType);
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(recordComponentDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        RecordComponentAttributeAppender.ForInstrumentedRecordComponent.INSTANCE.apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verify(recordComponentVisitor).visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.FIELD).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verifyNoMoreInteractions(recordComponentVisitor);
        verify(recordComponentDescription).getDeclaredAnnotations();
        verify(recordComponentDescription).getType();
        verifyNoMoreInteractions(recordComponentDescription);
    }

    @Test
    public void testFieldTypeTypeAnnotationByteCodeRetention() throws Exception {
        when(recordComponentDescription.getType()).thenReturn(simpleAnnotatedType);
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(recordComponentDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        RecordComponentAttributeAppender.ForInstrumentedRecordComponent.INSTANCE.apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verify(recordComponentVisitor).visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.FIELD).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verifyNoMoreInteractions(recordComponentVisitor);
        verify(recordComponentDescription).getDeclaredAnnotations();
        verify(recordComponentDescription).getType();
        verifyNoMoreInteractions(recordComponentDescription);
    }
}
