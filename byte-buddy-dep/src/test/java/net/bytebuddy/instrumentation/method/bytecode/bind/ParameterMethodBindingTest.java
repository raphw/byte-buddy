package net.bytebuddy.instrumentation.method.bytecode.bind;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.*;

public class ParameterMethodBindingTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private StackManipulation stackManipulation;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Instrumentation.Context instrumentationContext;

    @Before
    public void setUp() throws Exception {
        when(stackManipulation.isValid()).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testIllegal() throws Exception {
        assertThat(MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE.isValid(), is(false));
    }

    @Test
    public void testAnonymous() throws Exception {
        MethodDelegationBinder.ParameterBinding<?> parameterBinding =
                new MethodDelegationBinder.ParameterBinding.Anonymous(stackManipulation);
        assertThat(parameterBinding.getIdentificationToken(), notNullValue());
        assertThat(parameterBinding.isValid(), is(true));
        parameterBinding.apply(methodVisitor, instrumentationContext);
        verify(stackManipulation).apply(methodVisitor, instrumentationContext);
        verify(stackManipulation).isValid();
    }

    @Test
    public void testIdentified() throws Exception {
        Object identificationToken = new Object();
        MethodDelegationBinder.ParameterBinding<?> parameterBinding =
                MethodDelegationBinder.ParameterBinding.Unique.of(stackManipulation, identificationToken);
        assertThat(parameterBinding.getIdentificationToken(), notNullValue());
        assertThat(parameterBinding.isValid(), is(true));
        parameterBinding.apply(methodVisitor, instrumentationContext);
        verify(stackManipulation).apply(methodVisitor, instrumentationContext);
        verify(stackManipulation).isValid();
    }
}
