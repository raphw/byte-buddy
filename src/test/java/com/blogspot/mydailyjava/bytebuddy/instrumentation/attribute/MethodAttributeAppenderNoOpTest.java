package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import org.junit.Test;

import static org.mockito.Mockito.verifyZeroInteractions;

public class MethodAttributeAppenderNoOpTest extends AbstractMethodAttributeAppenderTest {

    @Test
    public void testNoOp() throws Exception {
        MethodAttributeAppender.NoOp.INSTANCE.apply(methodVisitor, methodDescription);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }
}
