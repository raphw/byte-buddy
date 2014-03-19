package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

public class FieldAttributeAppenderForLoadedFieldTest extends AbstractFieldAttributeAppenderTest {

    private static final String BAR = "bar";

    private static class Foo {

        @Qux
        @Baz
        @QuxBaz
        private Object bar;
    }

    private Field field;

    @Before
    public void setUp() throws Exception {
        field = Foo.class.getDeclaredField(BAR);
    }

    @Test
    public void testLoadedFieldAttributeAppender() throws Exception {
        FieldAttributeAppender fieldAttributeAppender = new FieldAttributeAppender.ForLoadedField(field);
        fieldAttributeAppender.apply(fieldVisitor, fieldDescription);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }
}
