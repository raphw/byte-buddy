package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.Serializable;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DefaultCallBinderTest extends AbstractAnnotationBinderTest<DefaultCall> {

    private static final Class<?> NON_INTERFACE_TYPE = Object.class, INTERFACE_TYPE = Serializable.class, VOID_TYPE = void.class;

    @Mock
    private TypeDescription targetParameterType, firstInterface, secondInterface;

    @Mock
    private MethodDescription.Token methodToken;

    @Mock
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    public DefaultCallBinderTest() {
        super(DefaultCall.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(targetParameterType);
        when(targetParameterType.asErasure()).thenReturn(targetParameterType);
        when(implementationTarget.invokeDefault(any(TypeDescription.class), eq(methodToken))).thenReturn(specialMethodInvocation);
        when(firstInterface.asErasure()).thenReturn(firstInterface);
        when(secondInterface.asErasure()).thenReturn(secondInterface);
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
        when(source.asToken()).thenReturn(methodToken);
        when(source.isSpecializableFor(firstInterface)).thenReturn(true);
        when(instrumentedType.getInterfaces()).thenReturn(new GenericTypeList.Explicit(Arrays.asList(firstInterface, secondInterface)));
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DefaultCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(implementationTarget).getTypeDescription();
        verify(implementationTarget).invokeDefault(firstInterface, methodToken);
        verifyNoMoreInteractions(implementationTarget);
    }

    @Test
    public void testImplicitLookupIsAmbiguous() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(true, false);
        doReturn(VOID_TYPE).when(annotation).targetType();
        when(source.asToken()).thenReturn(methodToken);
        when(source.isSpecializableFor(firstInterface)).thenReturn(true);
        when(source.isSpecializableFor(secondInterface)).thenReturn(true);
        when(instrumentedType.getInterfaces()).thenReturn(new GenericTypeList.Explicit(Arrays.asList(firstInterface, secondInterface)));
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DefaultCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
        verify(implementationTarget).getTypeDescription();
        verify(implementationTarget).invokeDefault(firstInterface, methodToken);
        verifyNoMoreInteractions(implementationTarget);
    }

    @Test
    public void testExplicitLookup() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        doReturn(INTERFACE_TYPE).when(annotation).targetType();
        when(source.asToken()).thenReturn(methodToken);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DefaultCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(implementationTarget).invokeDefault(new TypeDescription.ForLoadedType(INTERFACE_TYPE), methodToken);
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
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(DefaultCall.Binder.class).apply();
        ObjectPropertyAssertion.of(DefaultCall.Binder.DefaultMethodLocator.Implicit.class).apply();
        ObjectPropertyAssertion.of(DefaultCall.Binder.DefaultMethodLocator.Explicit.class).apply();
    }
}
