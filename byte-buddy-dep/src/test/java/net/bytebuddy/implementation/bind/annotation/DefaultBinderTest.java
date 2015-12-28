package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class DefaultBinderTest extends AbstractAnnotationBinderTest<Default> {

    @Mock
    private TypeDescription targetType;

    @Mock
    private TypeDescription.Generic typeDescription;

    @Mock
    private TypeList.Generic interfaces;

    @Mock
    private TypeList rawInterfaces;

    public DefaultBinderTest() {
        super(Default.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(typeDescription);
        when(typeDescription.asErasure()).thenReturn(targetType);
        when(instrumentedType.getInterfaces()).thenReturn(interfaces);
        when(interfaces.asErasures()).thenReturn(rawInterfaces);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Default> getSimpleBinder() {
        return Default.Binder.INSTANCE;
    }

    @Test
    public void testAssignableBinding() throws Exception {
        doReturn(void.class).when(annotation).proxyType();
        when(targetType.isInterface()).thenReturn(true);
        when(stackManipulation.isValid()).thenReturn(true);
        when(rawInterfaces.contains(targetType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Default.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testIllegalBindingNonDeclaredInterface() throws Exception {
        doReturn(void.class).when(annotation).proxyType();
        when(targetType.isInterface()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Default.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test
    public void testIllegalBindingStatic() throws Exception {
        doReturn(void.class).when(annotation).proxyType();
        when(targetType.isInterface()).thenReturn(true);
        when(source.isStatic()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Default.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testNonInterfaceProxyType() throws Exception {
        doReturn(void.class).when(annotation).proxyType();
        when(targetType.isInterface()).thenReturn(false);
        Default.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonInterfaceExplicitType() throws Exception {
        doReturn(Void.class).when(annotation).proxyType();
        Default.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Default.Binder.class).apply();
        ObjectPropertyAssertion.of(Default.Binder.TypeLocator.ForParameterType.class).apply();
        ObjectPropertyAssertion.of(Default.Binder.TypeLocator.ForType.class).apply();
    }
}
