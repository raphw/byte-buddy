package net.bytebuddy.instrumentation.attribute;

import org.junit.Test;
import org.mockito.asm.Type;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
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
    public void testHashCodeEquals() throws Exception {
        assertThat(new TypeAttributeAppender.ForType(FooBar.class).hashCode(), is(new TypeAttributeAppender.ForType(FooBar.class).hashCode()));
        assertThat(new TypeAttributeAppender.ForType(FooBar.class), is(new TypeAttributeAppender.ForType(FooBar.class)));
        assertThat(new TypeAttributeAppender.ForType(FooBar.class).hashCode(), not(is(new TypeAttributeAppender.ForType(Object.class).hashCode())));
        assertThat(new TypeAttributeAppender.ForType(FooBar.class), not(is(new TypeAttributeAppender.ForType(Object.class))));
    }

    @Baz
    @Qux
    @QuxBaz
    private static class FooBar {
        /* empty */
    }
}
