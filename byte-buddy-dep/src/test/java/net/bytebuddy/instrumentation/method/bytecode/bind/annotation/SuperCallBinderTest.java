package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
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
    private Instrumentation.SpecialMethodInvocation specialMethodInvocation;

    public SuperCallBinderTest() {
        super(SuperCall.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(target.getTypeDescription()).thenReturn(targetParameterType);
        when(instrumentationTarget.invokeSuper(eq(source), any(Instrumentation.Target.MethodLookup.class)))
                .thenReturn(specialMethodInvocation);
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
                .bind(annotationDescription, source, target, instrumentationTarget, assigner);
        verify(instrumentationTarget).invokeSuper(source, Instrumentation.Target.MethodLookup.Default.EXACT);
        verifyNoMoreInteractions(instrumentationTarget);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testInvalidSuperMethodCall() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(false);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = SuperCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, instrumentationTarget, assigner);
        verify(instrumentationTarget).invokeSuper(source, Instrumentation.Target.MethodLookup.Default.EXACT);
        verifyNoMoreInteractions(instrumentationTarget);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongTypeThrowsException() throws Exception {
        SuperCall.Binder.INSTANCE.bind(annotationDescription, source, target, instrumentationTarget, assigner);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SuperCall.Binder.class).apply();
    }
}
