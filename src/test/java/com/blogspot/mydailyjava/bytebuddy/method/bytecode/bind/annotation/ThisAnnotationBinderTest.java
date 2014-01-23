package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.LegalTrivialAssignment;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

public class ThisAnnotationBinderTest extends AbstractAnnotationBinderTest<This> {

    public ThisAnnotationBinderTest() {
        super(This.class);
    }

    @Test
    public void testAnnotationType() throws Exception {
        assertEquals(This.class, This.Binder.INSTANCE.getHandledType());
    }

    @Test
    public void testSimpleLegalBinding() throws Exception {
        final Class<?> instrumentedType = Object.class, targetType = Void.class;
        doReturn(instrumentedType).when(typeDescription).getSuperClass();
        when(target.getParameterTypes()).thenReturn(new Class<?>[]{targetType});
        when(target.getParameterAnnotations()).thenReturn(new Annotation[1][0]);
        when(assigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(LegalTrivialAssignment.INSTANCE);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding = This.Binder.INSTANCE
                .bind(annotation, 0, source, target, typeDescription, assigner);
        assertThat(identifiedBinding.isValid(), is(true));
        verify(assigner).assign(instrumentedType, targetType, false);
        verifyNoMoreInteractions(assigner);
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(typeDescription, atLeast(1)).getSuperClass();
    }

    @Test
    public void testSimpleLegalBindingRuntimeType() throws Exception {
        final Class<?> instrumentedType = Object.class, targetType = Void.class;
        doReturn(instrumentedType).when(typeDescription).getSuperClass();
        when(target.getParameterTypes()).thenReturn(new Class<?>[]{targetType});
        RuntimeType runtimeType = mock(RuntimeType.class);
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
        when(target.getParameterAnnotations()).thenReturn(new Annotation[][]{{runtimeType}});
        when(assigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(LegalTrivialAssignment.INSTANCE);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding = This.Binder.INSTANCE
                .bind(annotation, 0, source, target, typeDescription, assigner);
        assertThat(identifiedBinding.isValid(), is(true));
        verify(assigner).assign(instrumentedType, targetType, true);
        verifyNoMoreInteractions(assigner);
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(typeDescription, atLeast(1)).getSuperClass();
    }

    @Test
    public void testInterfaceLegalBinding() throws Exception {
        final Class<?> instrumentedType = Object.class, firstInterface = Integer.class, secondInterface = Float.class, targetType = Void.class;
        doReturn(instrumentedType).when(typeDescription).getSuperClass();
        when(typeDescription.getInterfaces()).thenReturn(Arrays.<Class<?>>asList(firstInterface, secondInterface));
        when(target.getParameterTypes()).thenReturn(new Class<?>[]{Void.class});
        when(target.getParameterAnnotations()).thenReturn(new Annotation[1][0]);
        when(assigner.assign(any(Class.class), any(Class.class), anyBoolean()))
                .thenReturn(IllegalAssignment.INSTANCE, IllegalAssignment.INSTANCE, LegalTrivialAssignment.INSTANCE);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding = This.Binder.INSTANCE
                .bind(annotation, 0, source, target, typeDescription, assigner);
        assertThat(identifiedBinding.isValid(), is(true));
        verify(assigner).assign(instrumentedType, targetType, false);
        verify(assigner).assign(firstInterface, targetType, false);
        verify(assigner).assign(secondInterface, targetType, false);
        verifyNoMoreInteractions(assigner);
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(typeDescription, atLeast(1)).getSuperClass();
        verify(typeDescription, atLeast(1)).getInterfaces();
    }

    @Test
    public void testIllegalBinding() throws Exception {
        final Class<?> instrumentedType = Object.class, targetType = Void.class;
        doReturn(instrumentedType).when(typeDescription).getSuperClass();
        when(target.getParameterTypes()).thenReturn(new Class<?>[]{targetType});
        when(target.getParameterAnnotations()).thenReturn(new Annotation[1][0]);
        when(assigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(IllegalAssignment.INSTANCE);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding = This.Binder.INSTANCE
                .bind(annotation, 0, source, target, typeDescription, assigner);
        assertThat(identifiedBinding.isValid(), is(false));
        verify(assigner).assign(instrumentedType, targetType, false);
        verifyNoMoreInteractions(assigner);
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(typeDescription, atLeast(1)).getSuperClass();
    }

    @Test(expected = IllegalStateException.class)
    public void testPrimitiveType() throws Exception {
        when(target.getParameterTypes()).thenReturn(new Class<?>[]{int.class});
        This.Binder.INSTANCE.bind(annotation, 0, source, target, typeDescription, assigner);
    }

    @Test(expected = IllegalStateException.class)
    public void testArrayType() throws Exception {
        when(target.getParameterTypes()).thenReturn(new Class<?>[]{Object[].class});
        This.Binder.INSTANCE.bind(annotation, 0, source, target, typeDescription, assigner);
    }
}
