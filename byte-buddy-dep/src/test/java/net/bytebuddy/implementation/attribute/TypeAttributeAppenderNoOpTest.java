package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verifyZeroInteractions;

public class TypeAttributeAppenderNoOpTest extends AbstractTypeAttributeAppenderTest {

    @Mock
    private TypeDescription typeDescription;

    @Test
    public void testNoOp() throws Exception {
        TypeAttributeAppender.NoOp.INSTANCE.apply(classVisitor, typeDescription);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.NoOp.class).apply();
    }
}