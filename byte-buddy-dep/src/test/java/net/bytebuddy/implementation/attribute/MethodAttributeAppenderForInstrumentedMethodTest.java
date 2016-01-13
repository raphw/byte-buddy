package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.asm.Type;
import org.objectweb.asm.TypeReference;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForInstrumentedMethodTest extends AbstractMethodAttributeAppenderTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodAnnotationNoRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
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
    public void testMethodAnnotationRuntimeRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodAnnotationClassFileRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodParameterAnnotationNoRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        ParameterDescription parameterDescription = mock(ParameterDescription.class);
        when(parameterDescription.getType()).thenReturn(TypeDescription.Generic.OBJECT);
        when(parameterDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Explicit<ParameterDescription>(parameterDescription));
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
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
    public void testMethodParameterAnnotationRuntimeRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        ParameterDescription parameterDescription = mock(ParameterDescription.class);
        when(parameterDescription.getType()).thenReturn(TypeDescription.Generic.OBJECT);
        when(parameterDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Explicit<ParameterDescription>(parameterDescription));
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitParameterAnnotation(0, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodParameterAnnotationClassFileRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        ParameterDescription parameterDescription = mock(ParameterDescription.class);
        when(parameterDescription.getType()).thenReturn(TypeDescription.Generic.OBJECT);
        when(parameterDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Explicit<ParameterDescription>(parameterDescription));
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitParameterAnnotation(0, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodReturnTypeTypeAnnotationNoRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(simpleAnnotatedType);
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
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
    public void testMethodReturnTypeTypeAnnotationRuntimeRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(simpleAnnotatedType);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.METHOD_RETURN).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodReturnTypeTypeAnnotationClassFileRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(simpleAnnotatedType);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.METHOD_RETURN).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodExceptionTypeTypeAnnotationNoRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Explicit(simpleAnnotatedType));
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
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
    public void testMethodExceptionTypeTypeAnnotationRuntimeRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Explicit(simpleAnnotatedType));
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newExceptionReference(0).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodExceptionTypeTypeAnnotationClassFileRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Explicit(simpleAnnotatedType));
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newExceptionReference(0).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodParameterTypeTypeAnnotationNoRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        ParameterDescription parameterDescription = mock(ParameterDescription.class);
        when(parameterDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(parameterDescription.getType()).thenReturn(simpleAnnotatedType);
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Explicit<ParameterDescription>(parameterDescription));
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
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
    public void testMethodParameterTypeTypeAnnotationRuntimeRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        ParameterDescription parameterDescription = mock(ParameterDescription.class);
        when(parameterDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(parameterDescription.getType()).thenReturn(simpleAnnotatedType);
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Explicit<ParameterDescription>(parameterDescription));
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newFormalParameterReference(0).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodParameterTypeTypeAnnotationClassFileRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        ParameterDescription parameterDescription = mock(ParameterDescription.class);
        when(parameterDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(parameterDescription.getType()).thenReturn(simpleAnnotatedType);
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Explicit<ParameterDescription>(parameterDescription));
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newFormalParameterReference(0).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTypeVariableTypeAnnotationNoRetention() throws Exception {
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(annotatedTypeVariable));
        when(annotatedTypeVariable.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(annotatedTypeVariableBound.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(annotatedTypeVariable));
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
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
    public void testTypeVariableTypeAnnotationRuntimeRetention() throws Exception {
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(annotatedTypeVariable));
        when(annotatedTypeVariable.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(annotatedTypeVariableBound.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(annotatedTypeVariable));
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeParameterReference(TypeReference.METHOD_TYPE_PARAMETER, 0).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeParameterBoundReference(TypeReference.METHOD_TYPE_PARAMETER_BOUND, 0, 0).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
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
    public void testTypeVariableTypeAnnotations() throws Exception {
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(annotatedTypeVariable));
        when(annotatedTypeVariable.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(annotatedTypeVariableBound.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(annotatedTypeVariable));
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeParameterReference(TypeReference.METHOD_TYPE_PARAMETER, 0).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeParameterBoundReference(TypeReference.METHOD_TYPE_PARAMETER_BOUND, 0, 0).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verifyZeroInteractions(methodVisitor);
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testFactory() throws Exception {
        assertThat(MethodAttributeAppender.ForInstrumentedMethod.INSTANCE.make(instrumentedType),
                sameInstance((MethodAttributeAppender) MethodAttributeAppender.ForInstrumentedMethod.INSTANCE));
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodAttributeAppender.ForInstrumentedMethod.class).apply();
    }
}
