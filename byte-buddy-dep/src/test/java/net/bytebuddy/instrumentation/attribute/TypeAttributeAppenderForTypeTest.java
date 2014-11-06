package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.asm.Type;

import static org.mockito.Mockito.*;

public class TypeAttributeAppenderForTypeTest extends AbstractTypeAttributeAppenderTest {

    @Test
    public void testTypeAnnotation() throws Exception {
        TypeAttributeAppender fieldAttributeAppender = new TypeAttributeAppender.ForType(FooBar.class);
        fieldAttributeAppender.apply(classVisitor, typeDescription);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(classVisitor);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.ForType.class).apply();
    }

    @Baz
    @Qux
    @QuxBaz
    private static class FooBar {
        /* empty */
    }
}
