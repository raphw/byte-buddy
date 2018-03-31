package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationList;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;

import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForReceiverTypeTest extends AbstractMethodAttributeAppenderTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testReceiverTypeAnnotationNoRetention() throws Exception {
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        new MethodAttributeAppender.ForReceiverType(simpleAnnotatedType).apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyZeroInteractions(methodVisitor);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodAnnotationRuntimeRetention() throws Exception {
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        new MethodAttributeAppender.ForReceiverType(simpleAnnotatedType).apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.METHOD_RECEIVER).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodAnnotationClassFileRetention() throws Exception {
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        new MethodAttributeAppender.ForReceiverType(simpleAnnotatedType).apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.METHOD_RECEIVER).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }
}
