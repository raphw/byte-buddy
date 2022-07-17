package net.bytebuddy.implementation.attribute;

import org.junit.Test;

import static org.mockito.Mockito.verifyNoMoreInteractions;

public class TypeAttributeAppenderNoOpTest extends AbstractTypeAttributeAppenderTest {

    @Test
    public void testApplication() throws Exception {
        TypeAttributeAppender.NoOp.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyNoMoreInteractions(classVisitor);
        verifyNoMoreInteractions(instrumentedType);
    }
}
