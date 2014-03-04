package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import org.junit.Test;

import static org.mockito.Mockito.verifyZeroInteractions;

public class FieldAttributeAppenderNoOpTest extends AbstractFieldAttributeAppenderTest {

    @Test
    public void testNoOp() throws Exception {
        FieldAttributeAppender.NoOp.INSTANCE.apply(fieldVisitor, fieldDescription);
        verifyZeroInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }
}
