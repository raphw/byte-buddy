package net.bytebuddy.implementation.bind;

import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.MockitoRule;
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

public class MethodDelegationBindingParameterBindingTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private StackManipulation stackManipulation;

    @Mock
    private StackManipulation.Size size;

    @Before
    public void setUp() throws Exception {
        when(stackManipulation.apply(methodVisitor, implementationContext)).thenReturn(size);
        when(stackManipulation.isValid()).thenReturn(true);
    }

    @Test
    public void testIllegalBindingIsInvalid() throws Exception {
        assertThat(MethodDelegationBinder.MethodBinding.Illegal.INSTANCE.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalBindingCannotExtractToken() throws Exception {
        MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE.getIdentificationToken();
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalBindingIsNotApplicable() throws Exception {
        MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE.apply(methodVisitor, implementationContext);
    }

    @Test
    public void testAnonymousToken() throws Exception {
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = new MethodDelegationBinder.ParameterBinding.Anonymous(stackManipulation);
        assertThat(parameterBinding.isValid(), is(true));
        assertThat(parameterBinding.getIdentificationToken(), notNullValue());
        assertThat(parameterBinding.apply(methodVisitor, implementationContext), is(size));
        verify(stackManipulation).isValid();
        verify(stackManipulation).apply(methodVisitor, implementationContext);
        verifyNoMoreInteractions(stackManipulation);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testUniqueToken() throws Exception {
        Object token = new Object();
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = new MethodDelegationBinder.ParameterBinding.Unique<Object>(stackManipulation, token);
        assertThat(parameterBinding.isValid(), is(true));
        assertThat(parameterBinding.getIdentificationToken(), is(token));
        assertThat(parameterBinding.apply(methodVisitor, implementationContext), is(size));
        verify(stackManipulation).isValid();
        verify(stackManipulation).apply(methodVisitor, implementationContext);
        verifyNoMoreInteractions(stackManipulation);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }
}
