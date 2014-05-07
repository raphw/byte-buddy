package net.bytebuddy.instrumentation.attribute;

import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class FieldAttributeAppenderForLoadedFieldTest extends AbstractFieldAttributeAppenderTest {

    private static final String BAR = "bar";
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

    @Test
    public void testHashCodeEquals() throws Exception {
        Field otherField = Other.class.getDeclaredField(BAR);
        assertThat(new FieldAttributeAppender.ForLoadedField(field).hashCode(), is(new FieldAttributeAppender.ForLoadedField(field).hashCode()));
        assertThat(new FieldAttributeAppender.ForLoadedField(field), is(new FieldAttributeAppender.ForLoadedField(field)));
        assertThat(new FieldAttributeAppender.ForLoadedField(field).hashCode(), not(is(new FieldAttributeAppender.ForLoadedField(otherField).hashCode())));
        assertThat(new FieldAttributeAppender.ForLoadedField(field), not(is(new FieldAttributeAppender.ForLoadedField(otherField))));
    }

    private static class Foo {

        @Qux
        @Baz
        @QuxBaz
        private Object bar;
    }

    private static class Other {

        private Object bar;
    }
}
