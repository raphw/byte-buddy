package net.bytebuddy.instrumentation.attribute;

import org.junit.Test;
import org.mockito.asm.Type;

import static org.mockito.Mockito.*;

public class FieldAttributeAppenderForAnnotationTest extends AbstractFieldAttributeAppenderTest {

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new FieldAttributeAppender.ForAnnotation(new Qux.Instance()).apply(fieldVisitor, fieldDescription);
        verifyZeroInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new FieldAttributeAppender.ForAnnotation(new Baz.Instance()).apply(fieldVisitor, fieldDescription);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new FieldAttributeAppender.ForAnnotation(new QuxBaz.Instance()).apply(fieldVisitor, fieldDescription);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }
}
