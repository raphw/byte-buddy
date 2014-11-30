package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipeBinderTest extends AbstractAnnotationBinderTest<Pipe> {

    private TargetMethodAnnotationDrivenBinder.ParameterBinder<Pipe> binder;
    @Mock
    private MethodDescription targetMethod;
    @Mock
    private TypeDescription targetMethodType;

    public PipeBinderTest() {
        super(Pipe.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(targetMethod.getDeclaringType()).thenReturn(targetMethodType);
        binder = new Pipe.Binder(targetMethod);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Pipe> getSimpleBinder() {
        return binder;
    }

    @Test
    public void testParameterBinding() throws Exception {
        when(targetTypeList.get(0)).thenReturn(targetMethodType);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = binder.bind(annotationDescription,
                0,
                source,
                target,
                instrumentationTarget,
                assigner);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testCannotPipeStaticMethod() throws Exception {
        when(targetTypeList.get(0)).thenReturn(targetMethodType);
        when(source.isStatic()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = binder.bind(annotationDescription,
                0,
                source,
                target,
                instrumentationTarget,
                assigner);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterBindingOnIllegalTargetTypeThrowsException() throws Exception {
        when(targetTypeList.get(0)).thenReturn(mock(TypeDescription.class));
        binder.bind(annotationDescription,
                0,
                source,
                target,
                instrumentationTarget,
                assigner);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Pipe.Binder.class).apply();
        ObjectPropertyAssertion.of(Pipe.Binder.PrecomputedFinding.class).apply();
        ObjectPropertyAssertion.of(Pipe.Binder.Redirection.class).apply();
        ObjectPropertyAssertion.of(Pipe.Binder.Redirection.MethodCall.class).skipSynthetic().apply();
    }

    @Test
    public void testRedirectionHashCodeEquals() throws Exception {
        MethodDescription sourceMethod = mock(MethodDescription.class);
        Assigner assigner = mock(Assigner.class);
        MethodLookupEngine.Factory factory = mock(MethodLookupEngine.Factory.class);
        Pipe.Binder.Redirection redirection = new Pipe.Binder.Redirection(targetMethodType,
                sourceMethod,
                assigner,
                false,
                factory);
        assertThat(redirection.hashCode(), is(new Pipe.Binder.Redirection(targetMethodType,
                sourceMethod,
                assigner,
                false,
                factory).hashCode()));
        assertThat(redirection, is(new Pipe.Binder.Redirection(targetMethodType,
                sourceMethod,
                assigner,
                false,
                factory)));
        assertThat(redirection.hashCode(), not(is(new Pipe.Binder.Redirection(targetMethodType,
                sourceMethod,
                assigner,
                true,
                factory).hashCode())));
        assertThat(redirection, not(is(new Pipe.Binder.Redirection(targetMethodType,
                sourceMethod,
                assigner,
                true,
                factory))));
    }
}
