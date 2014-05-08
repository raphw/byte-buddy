package net.bytebuddy.instrumentation.attribute;

import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.annotation.Annotation;

import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForInstrumentedMethodTest extends AbstractMethodAttributeAppenderTest {

    @Test
    public void testMethodAnnotations() throws Exception {
        when(methodDescription.getAnnotations()).thenReturn(new Annotation[]{ new Qux.Instance(), new Baz.Instance(), new QuxBaz.Instance() });
        when(methodDescription.getParameterAnnotations()).thenReturn(new Annotation[0][0]);
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getAnnotations();
        verify(methodDescription).getParameterAnnotations();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testMethodParameterAnnotations() throws Exception {
        when(methodDescription.getAnnotations()).thenReturn(new Annotation[0]);
        when(methodDescription.getParameterAnnotations()).thenReturn(new Annotation[][]{ { new Qux.Instance(), new Baz.Instance(), new QuxBaz.Instance() } });
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitParameterAnnotation(0, Type.getDescriptor(Baz.class), true);
        verify(methodVisitor).visitParameterAnnotation(0, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getAnnotations();
        verify(methodDescription).getParameterAnnotations();
        verifyNoMoreInteractions(methodDescription);
    }
}
