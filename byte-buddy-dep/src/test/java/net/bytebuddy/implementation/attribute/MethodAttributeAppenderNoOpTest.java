package net.bytebuddy.implementation.attribute;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.mockito.Mockito.verifyZeroInteractions;

public class MethodAttributeAppenderNoOpTest extends AbstractMethodAttributeAppenderTest {

    @Test
    public void testNoOp() throws Exception {
        MethodAttributeAppender.NoOp.INSTANCE.apply(methodVisitor, methodDescription);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodAttributeAppender.NoOp.class).apply();
    }
}
