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

public class IdentifiedBindingTest {

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
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding =
                AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding.makeIllegal();
        assertThat(identifiedBinding.getIdentificationToken(), notNullValue());
        assertThat(identifiedBinding.isValid(), is(false));
        assertThat(identifiedBinding.getStackManipulation().isValid(), is(false));
    }

    @Test
    public void testAnonymous() throws Exception {
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding =
                AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding.makeAnonymous(stackManipulation);
        assertThat(identifiedBinding.getStackManipulation(), is(stackManipulation));
        assertThat(identifiedBinding.getIdentificationToken(), notNullValue());
        assertThat(identifiedBinding.isValid(), is(true));
    }

    @Test
    public void testIdentified() throws Exception {
        Object token = new Object();
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding =
                AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding.makeIdentified(stackManipulation, token);
        assertThat(identifiedBinding.getStackManipulation(), is(stackManipulation));
        assertThat(identifiedBinding.getIdentificationToken(), is(token));
        assertThat(identifiedBinding.isValid(), is(true));
    }
}
