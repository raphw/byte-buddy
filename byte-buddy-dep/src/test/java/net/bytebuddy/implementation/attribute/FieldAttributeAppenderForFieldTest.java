package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class FieldAttributeAppenderForFieldTest extends AbstractFieldAttributeAppenderTest {

    private static final String FOO = "foo", BAR = "bar";

    private Field field;

    @Before
    public void setUp() throws Exception {
        field = Foo.class.getDeclaredField(BAR);
    }

    @Test
    public void testMakeReturnsSameInstance() throws Exception {
        FieldAttributeAppender.Factory factory = new FieldAttributeAppender.ForField(field, valueFilter);
        assertThat(factory.make(mock(TypeDescription.class)), is((FieldAttributeAppender) factory));
    }

    @Test
    public void testLoadedFieldAttributeAppender() throws Exception {
        FieldAttributeAppender fieldAttributeAppender = new FieldAttributeAppender.ForField(field, valueFilter);
        fieldAttributeAppender.apply(fieldVisitor, fieldDescription);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Field> iterator = Arrays.asList(Sample.class.getDeclaredField(FOO),
                Sample.class.getDeclaredField(BAR),
                Sample.class.getDeclaredField(FOO),
                Sample.class.getDeclaredField(BAR)).iterator();
        ObjectPropertyAssertion.of(FieldAttributeAppender.ForField.class).create(new ObjectPropertyAssertion.Creator<Field>() {
            @Override
            public Field create() {
                return iterator.next();
            }
        }).apply();
    }

    private static class Foo {

        @Qux
        @Baz
        @QuxBaz
        private Object bar;
    }

    private static class Sample {

        private Void foo, bar;
    }
}
