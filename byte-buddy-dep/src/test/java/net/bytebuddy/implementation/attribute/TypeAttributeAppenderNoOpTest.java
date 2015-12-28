package net.bytebuddy.implementation.attribute;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.mockito.Mockito.verifyZeroInteractions;

public class TypeAttributeAppenderNoOpTest extends AbstractTypeAttributeAppenderTest {

    @Test
    public void testNoOp() throws Exception {
        TypeAttributeAppender.NoOp.INSTANCE.apply(classVisitor, rawTypeDescription, targetType);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(rawTypeDescription);
        verifyZeroInteractions(targetType);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.NoOp.class).apply();
    }
}