package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.annotation.Annotation;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForAnnotationTest extends AbstractMethodAttributeAppenderTest {

    private static final int PARAMETER_INDEX = 0;

    @Before
    public void setUp() throws Exception {
        TypeList typeList = mock(TypeList.class);
        when(typeList.size()).thenReturn(PARAMETER_INDEX + 1);
        when(methodDescription.getParameterTypes()).thenReturn(typeList);
    }

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(new Qux.Instance()).apply(methodVisitor, methodDescription);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(new Baz.Instance()).apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(new QuxBaz.Instance()).apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterNoRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX, new Qux.Instance()).apply(methodVisitor, methodDescription);
        verifyZeroInteractions(methodVisitor);
        verify(methodDescription).getParameterTypes();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterRuntimeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX, new Baz.Instance()).apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameterTypes();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterByteCodeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX, new QuxBaz.Instance()).apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameterTypes();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAnnotationAppenderNotEnoughParameters() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX + 1, new Baz.Instance()).apply(methodVisitor, methodDescription);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        Annotation qux = new Qux.Instance(), baz = new Baz.Instance();
        assertThat(new MethodAttributeAppender.ForAnnotation(qux).hashCode(), is(new MethodAttributeAppender.ForAnnotation(qux).hashCode()));
        assertThat(new MethodAttributeAppender.ForAnnotation(qux), is(new MethodAttributeAppender.ForAnnotation(qux)));
        assertThat(new MethodAttributeAppender.ForAnnotation(qux).hashCode(), not(is(new MethodAttributeAppender.ForAnnotation(baz).hashCode())));
        assertThat(new MethodAttributeAppender.ForAnnotation(qux), not(is(new MethodAttributeAppender.ForAnnotation(baz))));
    }
}
