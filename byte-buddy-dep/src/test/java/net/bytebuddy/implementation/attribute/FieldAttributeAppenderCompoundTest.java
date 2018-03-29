package net.bytebuddy.implementation.attribute;

import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

public class FieldAttributeAppenderCompoundTest extends AbstractFieldAttributeAppenderTest {

    @Mock
    private FieldAttributeAppender first, second;

    @Test
    public void testApplication() throws Exception {
        FieldAttributeAppender fieldAttributeAppender = new FieldAttributeAppender.Compound(first, second);
        fieldAttributeAppender.apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verify(first).apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verifyNoMoreInteractions(first);
        verify(second).apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verifyNoMoreInteractions(second);
        verifyZeroInteractions(instrumentedType);
    }
}
