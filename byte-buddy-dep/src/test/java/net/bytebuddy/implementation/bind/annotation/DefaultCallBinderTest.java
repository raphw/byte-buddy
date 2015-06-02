package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
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

    private static final String FOO = "foo";

    private static final Class<?> NON_INTERFACE_TYPE = Object.class, INTERFACE_TYPE = Serializable.class, VOID_TYPE = void.class;

    @Mock
    private TypeDescription targetParameterType, interface1, interface2;

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
        when(implementationTarget.invokeDefault(any(TypeDescription.class), any(String.class)))
                .thenReturn(specialMethodInvocation);
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
        when(source.getUniqueSignature()).thenReturn(FOO);
        when(source.isSpecializableFor(interface1)).thenReturn(true);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Explicit(Arrays.asList(interface1, interface2)));
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DefaultCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(implementationTarget).getTypeDescription();
        verify(implementationTarget).invokeDefault(interface1, FOO);
        verifyNoMoreInteractions(implementationTarget);
    }

    @Test
    public void testImplicitLookupIsAmbiguous() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(true, false);
        doReturn(VOID_TYPE).when(annotation).targetType();
        when(source.getUniqueSignature()).thenReturn(FOO);
        when(source.isSpecializableFor(interface1)).thenReturn(true);
        when(source.isSpecializableFor(interface2)).thenReturn(true);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Explicit(Arrays.asList(interface1, interface2)));
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DefaultCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
        verify(implementationTarget).getTypeDescription();
        verify(implementationTarget).invokeDefault(interface1, FOO);
        verifyNoMoreInteractions(implementationTarget);
    }

    @Test
    public void testExplicitLookup() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        doReturn(INTERFACE_TYPE).when(annotation).targetType();
        when(source.getUniqueSignature()).thenReturn(FOO);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DefaultCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(implementationTarget).invokeDefault(new TypeDescription.ForLoadedType(INTERFACE_TYPE), FOO);
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
