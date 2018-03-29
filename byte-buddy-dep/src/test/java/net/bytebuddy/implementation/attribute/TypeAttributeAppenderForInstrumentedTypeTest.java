package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeList;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;

import static org.mockito.Mockito.*;

public class TypeAttributeAppenderForInstrumentedTypeTest extends AbstractTypeAttributeAppenderTest {

    @Test
    public void testAnnotationNoRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        TypeAttributeAppender.ForInstrumentedType.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyZeroInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getSuperClass();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testAnnotationByteCodeRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        TypeAttributeAppender.ForInstrumentedType.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyZeroInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getSuperClass();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testAnnotationClassFileRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        TypeAttributeAppender.ForInstrumentedType.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyZeroInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getSuperClass();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testSuperClassTypeAnnotationNoRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(instrumentedType.getSuperClass()).thenReturn(simpleAnnotatedType);
        TypeAttributeAppender.ForInstrumentedType.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyZeroInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getSuperClass();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testSuperClassTypeAnnotationByteCodeRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(instrumentedType.getSuperClass()).thenReturn(simpleAnnotatedType);
        TypeAttributeAppender.ForInstrumentedType.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newSuperTypeReference(-1).getValue(), null, Type.getDescriptor(Baz.class), true);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getSuperClass();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testSuperClassTypeAnnotationClassFileRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(simpleAnnotatedType.getDeclaredAnnotations())
                .thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(instrumentedType.getSuperClass()).thenReturn(simpleAnnotatedType);
        TypeAttributeAppender.ForInstrumentedType.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newSuperTypeReference(-1).getValue(), null, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getSuperClass();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testInterfaceTypeAnnotationNoRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(simpleAnnotatedType));
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        TypeAttributeAppender.ForInstrumentedType.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyZeroInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getSuperClass();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testInterfaceTypeAnnotationRuntimeRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(simpleAnnotatedType));
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        TypeAttributeAppender.ForInstrumentedType.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newSuperTypeReference(0).getValue(), null, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getSuperClass();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testInterfaceTypeAnnotations() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(simpleAnnotatedType));
        when(simpleAnnotatedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        TypeAttributeAppender.ForInstrumentedType.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newSuperTypeReference(0).getValue(), null, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getSuperClass();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testTypeVariableTypeAnnotationNoRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(annotatedTypeVariable));
        when(annotatedTypeVariable.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(annotatedTypeVariableBound.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        TypeAttributeAppender.ForInstrumentedType.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyZeroInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getSuperClass();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testTypeVariableTypeAnnotationRuntimeRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(annotatedTypeVariable));
        when(annotatedTypeVariable.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(annotatedTypeVariableBound.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        TypeAttributeAppender.ForInstrumentedType.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newTypeParameterReference(TypeReference.CLASS_TYPE_PARAMETER, 0).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newTypeParameterBoundReference(TypeReference.CLASS_TYPE_PARAMETER_BOUND, 0, 0).getValue(),
                null,
                Type.getDescriptor(Baz.class),
                true);
        verifyNoMoreInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getSuperClass();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testTypeVariableTypeAnnotations() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Explicit(annotatedTypeVariable));
        when(annotatedTypeVariable.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(annotatedTypeVariableBound.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        TypeAttributeAppender.ForInstrumentedType.INSTANCE.apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newTypeParameterReference(TypeReference.CLASS_TYPE_PARAMETER, 0).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verify(classVisitor).visitTypeAnnotation(TypeReference.newTypeParameterBoundReference(TypeReference.CLASS_TYPE_PARAMETER_BOUND, 0, 0).getValue(),
                null,
                Type.getDescriptor(QuxBaz.class),
                false);
        verifyNoMoreInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
        verify(instrumentedType).getSuperClass();
        verify(instrumentedType).getInterfaces();
        verify(instrumentedType).getTypeVariables();
        verifyNoMoreInteractions(instrumentedType);
    }
}
