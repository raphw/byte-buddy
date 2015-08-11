package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
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
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    @Mock
    private MethodDescription.Token sourceToken;

    public SuperCallBinderTest() {
        super(SuperCall.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(targetParameterType);
        when(implementationTarget.invokeSuper(sourceToken)).thenReturn(specialMethodInvocation);
        when(targetParameterType.asErasure()).thenReturn(targetParameterType);
        when(source.asToken()).thenReturn(sourceToken);
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
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        verify(implementationTarget).invokeSuper(sourceToken);
        verifyNoMoreInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testInvalidSuperMethodCall() throws Exception {
        when(targetParameterType.represents(any(Class.class))).thenReturn(true);
        when(specialMethodInvocation.isValid()).thenReturn(false);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = SuperCall.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        verify(implementationTarget).invokeSuper(sourceToken);
        verifyNoMoreInteractions(implementationTarget);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongTypeThrowsException() throws Exception {
        SuperCall.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SuperCall.Binder.class).apply();
    }
}
