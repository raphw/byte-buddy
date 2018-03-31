package net.bytebuddy.implementation.attribute;

import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

public class MethodAttributeAppenderCompoundTest extends AbstractMethodAttributeAppenderTest {

    @Mock
    private MethodAttributeAppender first, second;

    @Test
    public void testApplication() throws Exception {
        MethodAttributeAppender methodAttributeAppender = new MethodAttributeAppender.Compound(first, second);
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(first).apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyNoMoreInteractions(first);
        verify(second).apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyNoMoreInteractions(second);
        verifyZeroInteractions(instrumentedType);
    }
}
