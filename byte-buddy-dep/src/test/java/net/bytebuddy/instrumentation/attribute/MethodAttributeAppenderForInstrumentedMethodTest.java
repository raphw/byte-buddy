package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.method.ParameterDescription;
import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.asm.Type;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForInstrumentedMethodTest extends AbstractMethodAttributeAppenderTest {

    @Test
    public void testMakeReturnsSameInstance() throws Exception {
        assertThat(MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.make(mock(TypeDescription.class)),
                is((MethodAttributeAppender) MethodAttributeAppender.ForInstrumentedMethod.INSTANCE));
    }

    @Test
    public void testMethodAnnotations() throws Exception {
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList
                .ForLoadedAnnotation(new Qux.Instance(), new Baz.Instance(), new QuxBaz.Instance()));
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty());
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testMethodParameterAnnotations() throws Exception {
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        ParameterDescription parameterDescription = mock(ParameterDescription.class);
        when(parameterDescription.getDeclaredAnnotations())
                .thenReturn(new AnnotationList.ForLoadedAnnotation(new Qux.Instance(), new Baz.Instance(), new QuxBaz.Instance()));
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Explicit(Collections.singletonList(parameterDescription)));
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitParameterAnnotation(0, Type.getDescriptor(Baz.class), true);
        verify(methodVisitor).visitParameterAnnotation(0, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodAttributeAppender.ForInstrumentedMethod.class).apply();
    }
}
