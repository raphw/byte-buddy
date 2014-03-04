package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

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
        new MethodAttributeAppender.ForAnnotation(new Qux.Instance(), PARAMETER_INDEX).apply(methodVisitor, methodDescription);
        verifyZeroInteractions(methodVisitor);
        verify(methodDescription).getParameterTypes();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterRuntimeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(new Baz.Instance(), PARAMETER_INDEX).apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameterTypes();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterByteCodeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(new QuxBaz.Instance(), PARAMETER_INDEX).apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameterTypes();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAnnotationAppenderNotEnoughParameters() throws Exception {
        new MethodAttributeAppender.ForAnnotation(new Baz.Instance(), PARAMETER_INDEX + 1).apply(methodVisitor, methodDescription);
    }
}
