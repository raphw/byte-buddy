package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;

import java.util.Arrays;
import java.util.Random;

import static org.mockito.Mockito.*;

public class TypeAttributeAppenderForInstrumentedTypeDifferentiatingTest extends AbstractTypeAttributeAppenderTest {

    @Mock
    private TypeDescription.Generic pseudoType;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(pseudoType.asGenericType()).thenReturn(pseudoType);
    }

    @Test
    public void testAnnotationNoRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance(), new Qux.Instance()));
        new TypeAttributeAppender.ForInstrumentedType.Differentiating(1, 0, 0).apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyZeroInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testAnnotationByteCodeRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance(), new Baz.Instance()));
        new TypeAttributeAppender.ForInstrumentedType.Differentiating(1, 0, 0).apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyZeroInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testAnnotationClassFileRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance(), new QuxBaz.Instance()));
        new TypeAttributeAppender.ForInstrumentedType.Differentiating(1, 0, 0).apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyZeroInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testInterfaceTypeAnnotationNoRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(pseudoType, simpleAnnotatedType));
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        new TypeAttributeAppender.ForInstrumentedType.Differentiating(0, 0, 1).apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyZeroInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testInterfaceTypeAnnotationRuntimeRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(pseudoType, simpleAnnotatedType));
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        new TypeAttributeAppender.ForInstrumentedType.Differentiating(0, 0, 1).apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newSuperTypeReference(1).getValue(), null, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testInterfaceTypeAnnotations() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(pseudoType, simpleAnnotatedType));
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        new TypeAttributeAppender.ForInstrumentedType.Differentiating(0, 0, 1).apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newSuperTypeReference(1).getValue(), null, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testTypeVariableTypeAnnotationNoRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(pseudoType, annotatedTypeVariable));
        when(annotatedTypeVariable.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(annotatedTypeVariableBound.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        new TypeAttributeAppender.ForInstrumentedType.Differentiating(0, 1, 0).apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyZeroInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testTypeVariableTypeAnnotationRuntimeRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(pseudoType, annotatedTypeVariable));
        when(annotatedTypeVariable.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(annotatedTypeVariableBound.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        new TypeAttributeAppender.ForInstrumentedType.Differentiating(0, 1, 0).apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newTypeParameterReference(TypeReference.CLASS_TYPE_PARAMETER, 1).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newTypeParameterBoundReference(TypeReference.CLASS_TYPE_PARAMETER_BOUND, 1, 0).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verifyNoMoreInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testTypeVariableTypeAnnotations() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(pseudoType, annotatedTypeVariable));
        when(annotatedTypeVariable.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(annotatedTypeVariableBound.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        new TypeAttributeAppender.ForInstrumentedType.Differentiating(0, 1, 0).apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newTypeParameterReference(TypeReference.CLASS_TYPE_PARAMETER, 1).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newTypeParameterBoundReference(TypeReference.CLASS_TYPE_PARAMETER_BOUND, 1, 0).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verifyNoMoreInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.ForInstrumentedType.Differentiating.class).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                AnnotationDescription[] annotationDescription = new AnnotationDescription[new Random().nextInt(10000)];
                when(mock.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(Arrays.asList(annotationDescription)));
                when(mock.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
                when(mock.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
            }
        }).apply();
    }
}
