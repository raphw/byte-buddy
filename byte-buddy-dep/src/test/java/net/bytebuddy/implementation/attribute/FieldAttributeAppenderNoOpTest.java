package net.bytebuddy.implementation.attribute;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.mockito.Mockito.verifyZeroInteractions;

public class FieldAttributeAppenderNoOpTest extends AbstractFieldAttributeAppenderTest {

    @Test
    public void testNoOp() throws Exception {
        FieldAttributeAppender.NoOp.INSTANCE.apply(fieldVisitor, fieldDescription);
        verifyZeroInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldAttributeAppender.NoOp.class).apply();
    }
}
