package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(target.getTypeDescription()).thenReturn(targetType);
        when(annotation.strategy()).thenReturn(instantiation);
        when(instantiation.proxyFor(targetType, instrumentationTarget, annotationDescription)).thenReturn(stackManipulation);
        when(annotation.constructorParameters()).thenReturn(new Class<?>[0]);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Super> getSimpleBinder() {
        return Super.Binder.INSTANCE;
    }

    @Test
    public void testAssignableBinding() throws Exception {
        when(stackManipulation.isValid()).thenReturn(true);
        when(instrumentedType.isAssignableTo(targetType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Super.Binder.INSTANCE
                .bind(annotationDescription, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(instantiation).proxyFor(targetType, instrumentationTarget, annotationDescription);
    }

    @Test
    public void testIllegalBinding() throws Exception {
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Super.Binder.INSTANCE
                .bind(annotationDescription, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
    }
}
