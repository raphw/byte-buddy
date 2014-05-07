package net.bytebuddy.instrumentation.attribute;

import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.annotation.Annotation;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class TypeAttributeAppenderForAnnotationTest extends AbstractTypeAttributeAppenderTest {

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new TypeAttributeAppender.ForAnnotation(new Qux.Instance()).apply(classVisitor, typeDescription);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new TypeAttributeAppender.ForAnnotation(new Baz.Instance()).apply(classVisitor, typeDescription);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(classVisitor);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new TypeAttributeAppender.ForAnnotation(new QuxBaz.Instance()).apply(classVisitor, typeDescription);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        Annotation qux = new Qux.Instance(), baz = new Baz.Instance();
        assertThat(new TypeAttributeAppender.ForAnnotation(qux).hashCode(), is(new TypeAttributeAppender.ForAnnotation(qux).hashCode()));
        assertThat(new TypeAttributeAppender.ForAnnotation(qux), is(new TypeAttributeAppender.ForAnnotation(qux)));
        assertThat(new TypeAttributeAppender.ForAnnotation(qux).hashCode(), not(is(new TypeAttributeAppender.ForAnnotation(baz).hashCode())));
        assertThat(new TypeAttributeAppender.ForAnnotation(qux), not(is(new TypeAttributeAppender.ForAnnotation(baz))));
    }
}
