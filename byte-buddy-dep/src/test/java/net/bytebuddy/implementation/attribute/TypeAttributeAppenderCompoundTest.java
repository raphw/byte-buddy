package net.bytebuddy.implementation.attribute;

import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class TypeAttributeAppenderCompoundTest extends AbstractTypeAttributeAppenderTest {

    @Mock
    private TypeAttributeAppender first, second;

    @Test
    public void testApplication() throws Exception {
        TypeAttributeAppender typeAttributeAppender = new TypeAttributeAppender.Compound(first, second);
        typeAttributeAppender.apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(first).apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyNoMoreInteractions(first);
        verify(second).apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyNoMoreInteractions(second);
        verifyNoMoreInteractions(instrumentedType);
    }
}
