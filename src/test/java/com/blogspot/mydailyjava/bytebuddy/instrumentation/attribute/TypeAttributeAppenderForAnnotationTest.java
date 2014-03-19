package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import org.junit.Test;
import org.mockito.asm.Type;

import static org.mockito.Mockito.*;

public class TypeAttributeAppenderForAnnotationTest extends AbstractTypeAttributeAppenderTest {

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new TypeAttributeAppender.ForAnnotation(new Qux.Instance()).apply(classVisitor, typeDescription);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new TypeAttributeAppender.ForAnnotation(new Baz.Instance()).apply(classVisitor, typeDescription);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(classVisitor);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new TypeAttributeAppender.ForAnnotation(new QuxBaz.Instance()).apply(classVisitor, typeDescription);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verifyZeroInteractions(typeDescription);
    }
}
