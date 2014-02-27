package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.when;

public class ParameterBindingTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private StackManipulation stackManipulation;

    @Before
    public void setUp() throws Exception {
        when(stackManipulation.isValid()).thenReturn(true);
    }

    @Test
    public void testIllegal() throws Exception {
        TargetMethodAnnotationDrivenBinder.ArgumentBinder.ParameterBinding<?> parameterBinding =
                TargetMethodAnnotationDrivenBinder.ArgumentBinder.ParameterBinding.makeIllegal();
        assertThat(parameterBinding.getIdentificationToken(), notNullValue());
        assertThat(parameterBinding.isValid(), is(false));
        assertThat(parameterBinding.getStackManipulation().isValid(), is(false));
    }

    @Test
    public void testAnonymous() throws Exception {
        TargetMethodAnnotationDrivenBinder.ArgumentBinder.ParameterBinding<?> parameterBinding =
                TargetMethodAnnotationDrivenBinder.ArgumentBinder.ParameterBinding.makeAnonymous(stackManipulation);
        assertThat(parameterBinding.getStackManipulation(), is(stackManipulation));
        assertThat(parameterBinding.getIdentificationToken(), notNullValue());
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testIdentified() throws Exception {
        Object token = new Object();
        TargetMethodAnnotationDrivenBinder.ArgumentBinder.ParameterBinding<?> parameterBinding =
                TargetMethodAnnotationDrivenBinder.ArgumentBinder.ParameterBinding.makeIdentified(stackManipulation, token);
        assertThat(parameterBinding.getStackManipulation(), is(stackManipulation));
        assertThat(parameterBinding.getIdentificationToken(), is(token));
        assertThat(parameterBinding.isValid(), is(true));
    }
}
