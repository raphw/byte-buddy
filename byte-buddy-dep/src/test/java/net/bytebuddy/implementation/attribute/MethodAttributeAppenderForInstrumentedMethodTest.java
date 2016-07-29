package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationDescriptionBuilderTest;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.asm.Type;
import org.objectweb.asm.TypeReference;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class MethodAttributeAppenderForInstrumentedMethodTest extends AbstractMethodAttributeAppenderTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {MethodAttributeAppender.ForInstrumentedMethod.EXCLUDING_RECEIVER},
                {MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER}
        });
    }

    private final MethodAttributeAppender methodAttributeAppender;

    public MethodAttributeAppenderForInstrumentedMethodTest(MethodAttributeAppender methodAttributeAppender) {
        this.methodAttributeAppender = methodAttributeAppender;
    }

    @After
    public void tearDown() throws Exception {
        verify(methodDescription).getDeclaredAnnotations();
        verify(methodDescription).getParameters();
        verify(methodDescription).getReturnType();
        verify(methodDescription).getExceptionTypes();
        verify(methodDescription).getTypeVariables();
        if (methodAttributeAppender == MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER) {
            verify(methodDescription).getReceiverType();
        }
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodAnnotationNoRetention() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyZeroInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyZeroInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitParameterAnnotation(0, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitParameterAnnotation(0, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyZeroInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.METHOD_RETURN).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verifyNoMoreInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.METHOD_RETURN).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verifyNoMoreInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyZeroInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newExceptionReference(0).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verifyNoMoreInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newExceptionReference(0).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verifyNoMoreInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyZeroInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newFormalParameterReference(0).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verifyNoMoreInteractions(methodVisitor);
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
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newFormalParameterReference(0).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTypeVariableTypeAnnotationNoRetention() throws Exception {
        when(annotatedTypeVariable.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(annotatedTypeVariableBound.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(annotatedTypeVariable));
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTypeVariableTypeAnnotationRuntimeRetention() throws Exception {
        when(annotatedTypeVariable.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(annotatedTypeVariableBound.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(annotatedTypeVariable));
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeParameterReference(TypeReference.METHOD_TYPE_PARAMETER, 0).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeParameterBoundReference(TypeReference.METHOD_TYPE_PARAMETER_BOUND, 0, 0).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTypeVariableTypeAnnotations() throws Exception {
        when(annotatedTypeVariable.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(annotatedTypeVariableBound.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(annotatedTypeVariable));
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeParameterReference(TypeReference.METHOD_TYPE_PARAMETER, 0).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verify(methodVisitor).visitTypeAnnotation(TypeReference.newTypeParameterBoundReference(TypeReference.METHOD_TYPE_PARAMETER_BOUND, 0, 0).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJdkTypeIsFiltered() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        AnnotationDescription annotationDescription = mock(AnnotationDescription.class);
        TypeDescription annotationType = mock(TypeDescription.class);
        when(annotationType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(Baz.class.getDeclaredAnnotations()));
        when(annotationDescription.getAnnotationType()).thenReturn(annotationType);
        when(annotationType.getActualName()).thenReturn("jdk.Sample");
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(annotationDescription));
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(methodDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyZeroInteractions(methodVisitor);
    }
}
