package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class DefaultBinderTest extends AbstractAnnotationBinderTest<Default> {

    @Mock
    private TypeDescription targetType;

    @Mock
    private TypeList interfaces;

    public DefaultBinderTest() {
        super(Default.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(target.getTypeDescription()).thenReturn(targetType);
        when(instrumentedType.getInterfaces()).thenReturn(interfaces);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Default> getSimpleBinder() {
        return Default.Binder.INSTANCE;
    }

    @Test
    public void testAssignableBinding() throws Exception {
        when(targetType.isInterface()).thenReturn(true);
        when(stackManipulation.isValid()).thenReturn(true);
        when(interfaces.contains(targetType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Default.Binder.INSTANCE
                .bind(annotationDescription, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testIllegalBinding() throws Exception {
        when(targetType.isInterface()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Default.Binder.INSTANCE
                .bind(annotationDescription, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalAnnotation() throws Exception {
        Default.Binder.INSTANCE.bind(annotationDescription, source, target, instrumentationTarget, assigner);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Default.Binder.class).apply();
    }
}
