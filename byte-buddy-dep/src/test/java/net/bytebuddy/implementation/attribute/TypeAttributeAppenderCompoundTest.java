package net.bytebuddy.implementation.attribute;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
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
        typeAttributeAppender.apply(classVisitor, typeDescription, targetType);
        verify(first).apply(classVisitor, typeDescription, targetType);
        verifyNoMoreInteractions(first);
        verify(second).apply(classVisitor, typeDescription, targetType);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.Compound.class).apply();
    }
}
