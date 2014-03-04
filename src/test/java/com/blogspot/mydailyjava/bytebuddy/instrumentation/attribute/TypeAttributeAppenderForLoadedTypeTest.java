package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import org.junit.Test;
import org.mockito.asm.Type;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

public class TypeAttributeAppenderForLoadedTypeTest extends AbstractTypeAttributeAppenderTest {

    @Baz
    @Qux
    @QuxBaz
    private static class FooBar {
        /* empty */
    }

    @Test
    public void testLoadedTypeAnnotation() throws Exception {
        TypeAttributeAppender fieldAttributeAppender = new TypeAttributeAppender.ForLoadedType(FooBar.class);
        fieldAttributeAppender.apply(classVisitor, typeDescription);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(classVisitor);
        verifyZeroInteractions(typeDescription);
    }
}
