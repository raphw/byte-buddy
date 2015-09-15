package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.asm.Type;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForInstrumentedMethodTest extends AbstractMethodAttributeAppenderTest {

    @Mock
    private TypeDescription instrumentedType;

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodAnnotations() throws Exception {
        when(valueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList
                .ForLoadedAnnotation(new Qux.Instance(), new Baz.Instance(), new QuxBaz.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty());
        when(methodDescription.getDeclaringType()).thenReturn(mock(GenericTypeDescription.class));
        MethodAttributeAppender methodAttributeAppender = new MethodAttributeAppender.ForInstrumentedMethod(valueFilter).make(instrumentedType);
        methodAttributeAppender.apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getDeclaringType();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodParameterAnnotations() throws Exception {
        when(valueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        ParameterDescription parameterDescription = mock(ParameterDescription.class);
        when(parameterDescription.getDeclaredAnnotations())
                .thenReturn(new AnnotationList.ForLoadedAnnotation(new Qux.Instance(), new Baz.Instance(), new QuxBaz.Instance()));
        when(methodDescription.getParameters())
                .thenReturn((ParameterList) new ParameterList.Explicit<ParameterDescription>(Collections.singletonList(parameterDescription)));
        when(methodDescription.getDeclaringType()).thenReturn(mock(GenericTypeDescription.class));
        MethodAttributeAppender methodAttributeAppender = new MethodAttributeAppender.ForInstrumentedMethod(valueFilter).make(instrumentedType);
        methodAttributeAppender.apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitParameterAnnotation(0, Type.getDescriptor(Baz.class), true);
        verify(methodVisitor).visitParameterAnnotation(0, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getDeclaringType();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testDoesNotApplyForDeclaredMethod() throws Exception {
        when(methodDescription.getDeclaringType()).thenReturn(instrumentedType);
        MethodAttributeAppender methodAttributeAppender = new MethodAttributeAppender.ForInstrumentedMethod(valueFilter).make(instrumentedType);
        methodAttributeAppender.apply(methodVisitor, methodDescription);
        verifyZeroInteractions(methodVisitor);
        verify(methodDescription).getDeclaringType();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodAttributeAppender.ForInstrumentedMethod.class).apply();
        ObjectPropertyAssertion.of(MethodAttributeAppender.ForInstrumentedMethod.Appender.class).apply();
    }
}
