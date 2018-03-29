package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class SuperCallBinderTest extends AbstractAnnotationBinderTest<SuperCall> {

    @Mock
    private TypeDescription targetParameterType;

    @Mock
    private TypeDescription.Generic genericTargetParameterType;

    @Mock
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    @Mock
    private MethodDescription.SignatureToken sourceToken;

    public SuperCallBinderTest() {
        super(SuperCall.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(genericTargetParameterType);
        when(genericTargetParameterType.asErasure()).thenReturn(targetParameterType);
        when(source.asSignatureToken()).thenReturn(sourceToken);
        when(implementationTarget.invokeSuper(sourceToken)).thenReturn(specialMethodInvocation);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<SuperCall> getSimpleBinder() {
        return SuperCall.Binder.INSTANCE;
    }

    @Test
    public void testValidSuperMethodCall() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = SuperCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        verify(implementationTarget).invokeSuper(sourceToken);
        verifyNoMoreInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testInvalidSuperMethodCall() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(false);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = SuperCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        verify(implementationTarget).invokeSuper(sourceToken);
        verifyNoMoreInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test
    public void testInvalidSuperMethodCallNullFallback() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(false);
        when(annotation.nullIfImpossible()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = SuperCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        verify(implementationTarget).invokeSuper(sourceToken);
        verifyNoMoreInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongTypeThrowsException() throws Exception {
        SuperCall.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @Test
    public void testConstructorIsNotInvokeable() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(source.isConstructor()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = SuperCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        verifyZeroInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test
    public void testConstructorNullFallback() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(source.isConstructor()).thenReturn(true);
        when(annotation.nullIfImpossible()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = SuperCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        verifyZeroInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(true));
    }
}
