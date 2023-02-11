package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class SuperHandleBinderTest extends AbstractAnnotationBinderTest<SuperCallHandle> {

    @Mock
    private TypeDescription targetParameterType;

    @Mock
    private TypeDescription.Generic genericTargetParameterType;

    @Mock
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    @Mock
    private MethodDescription.SignatureToken sourceToken;

    @Mock
    private ParameterDescription.InDefinedShape parameterDescription;

    @Mock
    private JavaConstant.MethodHandle methodHandle;

    public SuperHandleBinderTest() {
        super(SuperCallHandle.class);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(genericTargetParameterType);
        when(genericTargetParameterType.asErasure()).thenReturn(targetParameterType);
        when(source.asSignatureToken()).thenReturn(sourceToken);
        when(implementationTarget.invokeSuper(sourceToken)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.withCheckedCompatibilityTo(sourceTypeToken)).thenReturn(specialMethodInvocation);
        when(source.getParameters()).thenReturn(new ParameterList.Explicit<ParameterDescription.InDefinedShape>(parameterDescription));
        when(parameterDescription.getType()).thenReturn(TypeDescription.ForLoadedType.of(Object.class).asGenericType());
        when(specialMethodInvocation.toMethodHandle()).thenReturn(methodHandle);
        when(methodHandle.toStackManipulation()).thenReturn(StackManipulation.Trivial.INSTANCE);
    }

    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<SuperCallHandle> getSimpleBinder() {
        return SuperCallHandle.Binder.INSTANCE;
    }


    @Test
    public void testValidSuperMethodCall() throws Exception {
        when(targetParameterType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = SuperCallHandle.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        verify(implementationTarget).invokeSuper(sourceToken);
        verifyNoMoreInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testInvalidSuperMethodCall() throws Exception {
        when(targetParameterType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(false);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = SuperCallHandle.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        verify(implementationTarget).invokeSuper(sourceToken);
        verifyNoMoreInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test
    public void testInvalidSuperMethodCallNullFallback() throws Exception {
        when(targetParameterType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(false);
        when(annotation.nullIfImpossible()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = SuperCallHandle.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        verify(implementationTarget).invokeSuper(sourceToken);
        verifyNoMoreInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongTypeThrowsException() throws Exception {
        SuperCallHandle.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @Test
    public void testConstructorIsNotInvokeable() throws Exception {
        when(targetParameterType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(source.isConstructor()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = SuperCallHandle.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        verifyNoMoreInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test
    public void testConstructorNullFallback() throws Exception {
        when(targetParameterType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(source.isConstructor()).thenReturn(true);
        when(annotation.nullIfImpossible()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = SuperCallHandle.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        verifyNoMoreInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(true));
    }
}
