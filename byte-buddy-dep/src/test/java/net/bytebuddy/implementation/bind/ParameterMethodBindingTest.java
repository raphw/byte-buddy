package net.bytebuddy.implementation.bind;

import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ParameterMethodBindingTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private StackManipulation stackManipulation;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Before
    public void setUp() throws Exception {
        when(stackManipulation.isValid()).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
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
        parameterBinding.apply(methodVisitor, implementationContext);
        verify(stackManipulation).apply(methodVisitor, implementationContext);
        verify(stackManipulation).isValid();
    }

    @Test
    public void testIdentified() throws Exception {
        Object identificationToken = new Object();
        MethodDelegationBinder.ParameterBinding<?> parameterBinding =
                MethodDelegationBinder.ParameterBinding.Unique.of(stackManipulation, identificationToken);
        assertThat(parameterBinding.getIdentificationToken(), notNullValue());
        assertThat(parameterBinding.isValid(), is(true));
        parameterBinding.apply(methodVisitor, implementationContext);
        verify(stackManipulation).apply(methodVisitor, implementationContext);
        verify(stackManipulation).isValid();
    }
}
