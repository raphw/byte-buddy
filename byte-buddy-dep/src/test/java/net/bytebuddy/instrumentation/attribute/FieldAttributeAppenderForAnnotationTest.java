package net.bytebuddy.instrumentation.attribute;

import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.annotation.Annotation;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class FieldAttributeAppenderForAnnotationTest extends AbstractFieldAttributeAppenderTest {

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new FieldAttributeAppender.ForAnnotation(new Qux.Instance()).apply(fieldVisitor, fieldDescription);
        verifyZeroInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new FieldAttributeAppender.ForAnnotation(new Baz.Instance()).apply(fieldVisitor, fieldDescription);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new FieldAttributeAppender.ForAnnotation(new QuxBaz.Instance()).apply(fieldVisitor, fieldDescription);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        Annotation qux = new Qux.Instance(), baz = new Baz.Instance();
        assertThat(new FieldAttributeAppender.ForAnnotation(qux).hashCode(), is(new FieldAttributeAppender.ForAnnotation(qux).hashCode()));
        assertThat(new FieldAttributeAppender.ForAnnotation(qux), is(new FieldAttributeAppender.ForAnnotation(qux)));
        assertThat(new FieldAttributeAppender.ForAnnotation(qux).hashCode(), not(is(new FieldAttributeAppender.ForAnnotation(baz).hashCode())));
        assertThat(new FieldAttributeAppender.ForAnnotation(qux), not(is(new FieldAttributeAppender.ForAnnotation(baz))));
    }
}
