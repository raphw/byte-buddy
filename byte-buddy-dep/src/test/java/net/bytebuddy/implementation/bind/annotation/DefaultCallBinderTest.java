package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DefaultCallBinderTest extends AbstractAnnotationBinderTest<DefaultCall> {

    private static final Class<?> NON_INTERFACE_TYPE = Object.class, INTERFACE_TYPE = Serializable.class, VOID_TYPE = void.class;

    @Mock
    private TypeDescription targetParameterType, firstInterface, secondInterface;

    @Mock
    private TypeDescription.Generic genericTargetParameterType, firstGenericInterface, secondGenericInterface;

    @Mock
    private MethodDescription.SignatureToken token;

    @Mock
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    public DefaultCallBinderTest() {
        super(DefaultCall.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(genericTargetParameterType);
        when(genericTargetParameterType.asErasure()).thenReturn(targetParameterType);
        when(implementationTarget.invokeDefault(any(TypeDescription.class), eq(token))).thenReturn(specialMethodInvocation);
        when(firstGenericInterface.asErasure()).thenReturn(firstInterface);
        when(secondGenericInterface.asErasure()).thenReturn(secondInterface);
        when(firstInterface.asGenericType()).thenReturn(firstGenericInterface);
        when(secondInterface.asGenericType()).thenReturn(secondGenericInterface);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<DefaultCall> getSimpleBinder() {
        return DefaultCall.Binder.INSTANCE;
    }

    @Test
    public void testImplicitLookupIsUnique() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(true, false);
        doReturn(VOID_TYPE).when(annotation).targetType();
        when(source.asSignatureToken()).thenReturn(token);
        when(source.isSpecializableFor(firstInterface)).thenReturn(true);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(firstInterface, secondInterface));
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DefaultCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(implementationTarget).getInstrumentedType();
        verify(implementationTarget).invokeDefault(firstInterface, token);
        verifyNoMoreInteractions(implementationTarget);
    }

    @Test
    public void testImplicitLookupIsAmbiguous() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(true, false);
        doReturn(VOID_TYPE).when(annotation).targetType();
        when(source.asSignatureToken()).thenReturn(token);
        when(source.isSpecializableFor(firstInterface)).thenReturn(true);
        when(source.isSpecializableFor(secondInterface)).thenReturn(true);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(firstInterface, secondInterface));
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DefaultCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
        verify(implementationTarget).getInstrumentedType();
        verify(implementationTarget).invokeDefault(firstInterface, token);
        verifyNoMoreInteractions(implementationTarget);
    }

    @Test
    public void testImplicitLookupIsAmbiguousNullFallback() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(true, false);
        doReturn(VOID_TYPE).when(annotation).targetType();
        when(source.asSignatureToken()).thenReturn(token);
        when(source.isSpecializableFor(firstInterface)).thenReturn(true);
        when(source.isSpecializableFor(secondInterface)).thenReturn(true);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(firstInterface, secondInterface));
        when(annotation.nullIfImpossible()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DefaultCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(implementationTarget).getInstrumentedType();
        verify(implementationTarget).invokeDefault(firstInterface, token);
        verifyNoMoreInteractions(implementationTarget);
    }

    @Test
    public void testExplicitLookup() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        doReturn(INTERFACE_TYPE).when(annotation).targetType();
        when(source.asSignatureToken()).thenReturn(token);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DefaultCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(implementationTarget).invokeDefault(new TypeDescription.ForLoadedType(INTERFACE_TYPE), token);
        verifyNoMoreInteractions(implementationTarget);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonInterfaceTarget() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        doReturn(NON_INTERFACE_TYPE).when(annotation).targetType();
        DefaultCall.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalAnnotatedValue() throws Exception {
        DefaultCall.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test
    public void testConstructorIsNotInvokeable() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(source.isConstructor()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DefaultCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        verifyZeroInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test
    public void testConstructorNullFallback() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(source.isConstructor()).thenReturn(true);
        when(annotation.nullIfImpossible()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DefaultCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        verifyZeroInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(DefaultCall.Binder.class).apply();
        ObjectPropertyAssertion.of(DefaultCall.Binder.DefaultMethodLocator.Implicit.class).apply();
        ObjectPropertyAssertion.of(DefaultCall.Binder.DefaultMethodLocator.Explicit.class).apply();
    }
}
