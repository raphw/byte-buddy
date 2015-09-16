package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class SuperBinderTest extends AbstractAnnotationBinderTest<Super> {

    @Mock
    private TypeDescription targetType;

    @Mock
    private Super.Instantiation instantiation;

    public SuperBinderTest() {
        super(Super.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(targetType);
        when(annotation.strategy()).thenReturn(instantiation);
        when(instantiation.proxyFor(targetType, implementationTarget, annotationDescription)).thenReturn(stackManipulation);
        when(annotation.constructorParameters()).thenReturn(new Class<?>[0]);
        when(targetType.asErasure()).thenReturn(targetType);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Super> getSimpleBinder() {
        return Super.Binder.INSTANCE;
    }

    @Test
    public void testAssignableBinding() throws Exception {
        doReturn(void.class).when(annotation).proxyType();
        when(stackManipulation.isValid()).thenReturn(true);
        when(instrumentedType.isAssignableTo(targetType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Super.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(instantiation).proxyFor(targetType, implementationTarget, annotationDescription);
    }

    @Test
    public void testIllegalBindingForNonAssignableType() throws Exception {
        doReturn(void.class).when(annotation).proxyType();
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Super.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test
    public void testIllegalBindingStaticMethod() throws Exception {
        doReturn(void.class).when(annotation).proxyType();
        when(source.isStatic()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Super.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testPrimitiveParameterType() throws Exception {
        when(targetType.isPrimitive()).thenReturn(true);
        Super.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test(expected = IllegalStateException.class)
    public void testArrayParameterType() throws Exception {
        when(targetType.isArray()).thenReturn(true);
        Super.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test(expected = IllegalStateException.class)
    public void testPrimitiveProxyType() throws Exception {
        doReturn(int.class).when(annotation).proxyType();
        Super.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test(expected = IllegalStateException.class)
    public void testArrayProxyType() throws Exception {
        doReturn(Object[].class).when(annotation).proxyType();
        Super.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonAssignableType() throws Exception {
        doReturn(Void.class).when(annotation).proxyType();
        Super.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Super.Binder.class).apply();
        ObjectPropertyAssertion.of(Super.Instantiation.class).apply();
        ObjectPropertyAssertion.of(Super.Binder.TypeLocator.ForInstrumentedType.class).apply();
        ObjectPropertyAssertion.of(Super.Binder.TypeLocator.ForParameterType.class).apply();
        ObjectPropertyAssertion.of(Super.Binder.TypeLocator.ForType.class).apply();
    }
}
