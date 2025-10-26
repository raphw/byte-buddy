package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ThisBinderTest extends AbstractAnnotationBinderTest<This> {

    @Mock
    private TypeDescription.Generic parameterType, genericInstrumentedType;

    public ThisBinderTest() {
        super(This.class);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(stackManipulation.isValid()).thenReturn(true);
        when(instrumentedType.asGenericType()).thenReturn(genericInstrumentedType);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<This> getSimpleBinder() {
        return This.Binder.INSTANCE;
    }

    @Test
    public void testLegalBinding() throws Exception {
        when(stackManipulation.isValid()).thenReturn(true);
        when(target.getType()).thenReturn(parameterType);
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = This.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
        verify(assigner).assign(genericInstrumentedType, parameterType, Assigner.Typing.STATIC);
        verifyNoMoreInteractions(assigner);
        verify(target, atLeast(1)).getType();
        verify(target, never()).getDeclaredAnnotations();
    }

    @Test
    public void testLegalBindingRuntimeType() throws Exception {
        when(stackManipulation.isValid()).thenReturn(true);
        when(target.getType()).thenReturn(parameterType);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = This.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.DYNAMIC);
        assertThat(parameterBinding.isValid(), is(true));
        verify(assigner).assign(genericInstrumentedType, parameterType, Assigner.Typing.DYNAMIC);
        verifyNoMoreInteractions(assigner);
        verify(target, atLeast(1)).getType();
        verify(target, never()).getDeclaredAnnotations();
    }

    @Test
    public void testIllegalBinding() throws Exception {
        when(stackManipulation.isValid()).thenReturn(false);
        when(target.getType()).thenReturn(parameterType);
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(assigner.assign(any(TypeDescription.Generic.class), any(TypeDescription.Generic.class), any(Assigner.Typing.class)))
                .thenReturn(StackManipulation.Illegal.INSTANCE);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = This.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(false));
        verify(assigner).assign(genericInstrumentedType, parameterType, Assigner.Typing.STATIC);
        verifyNoMoreInteractions(assigner);
        verify(target, atLeast(1)).getType();
        verify(target, never()).getDeclaredAnnotations();
    }

    @Test
    public void testOptionalBinding() throws Exception {
        when(stackManipulation.isValid()).thenReturn(true);
        when(annotation.optional()).thenReturn(true);
        when(source.isStatic()).thenReturn(true);
        when(target.getType()).thenReturn(parameterType);
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = This.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
        verify(annotation).optional();
        verify(source, atLeast(1)).isStatic();
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testStaticMethodIllegal() throws Exception {
        when(target.getType()).thenReturn(parameterType);
        when(source.isStatic()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = This.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testPrimitiveType() throws Exception {
        when(parameterType.isPrimitive()).thenReturn(true);
        when(target.getType()).thenReturn(parameterType);
        This.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @Test(expected = IllegalStateException.class)
    public void testArrayType() throws Exception {
        when(parameterType.isArray()).thenReturn(true);
        when(target.getType()).thenReturn(parameterType);
        This.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }
}
