package net.bytebuddy.implementation.attribute;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.mockito.Mockito.verifyZeroInteractions;

public class TypeAttributeAppenderNoOpTest extends AbstractTypeAttributeAppenderTest {

    @Test
    public void testApplication() throws Exception {
        TypeAttributeAppender.NoOp.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.NoOp.class).apply();
    }
}
