package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.asm.Type;
import org.objectweb.asm.TypeReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForInstrumentedMethodOtherTest extends AbstractMethodAttributeAppenderTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testReceiverTypeTypeAnnotationsIgnored() throws Exception {
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(methodDescription.getReceiverType()).thenReturn(simpleAnnotatedType);
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        MethodAttributeAppender.ForInstrumentedMethod.EXCLUDING_RECEIVER.apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyZeroInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReceiverTypeTypeAnnotationsNoRetention() throws Exception {
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(methodDescription.getReceiverType()).thenReturn(simpleAnnotatedType);
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER.apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyZeroInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verify(methodDescription).getReceiverType();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReceiverTypeTypeAnnotationsRuntimeRetention() throws Exception {
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(methodDescription.getReceiverType()).thenReturn(simpleAnnotatedType);
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.METHOD_RECEIVER).getValue(),
                null,
                Type.getDescriptor(AbstractAttributeAppenderTest.Baz.class),
                true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verify(methodDescription).getReceiverType();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReceiverTypeTypeAnnotationsClassFileRetention() throws Exception {
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(methodDescription.getReceiverType()).thenReturn(simpleAnnotatedType);
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.METHOD_RECEIVER).getValue(),
                null,
                Type.getDescriptor(AbstractAttributeAppenderTest.QuxBaz.class),
                false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verify(methodDescription).getReceiverType();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testIncludingFactory() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        assertThat(MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER.make(typeDescription),
                is((MethodAttributeAppender) MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER));
    }

    @Test
    public void testExcludingFactory() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        assertThat(MethodAttributeAppender.ForInstrumentedMethod.EXCLUDING_RECEIVER.make(typeDescription),
                is((MethodAttributeAppender) MethodAttributeAppender.ForInstrumentedMethod.EXCLUDING_RECEIVER));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodAttributeAppender.ForInstrumentedMethod.class).apply();
    }
}
