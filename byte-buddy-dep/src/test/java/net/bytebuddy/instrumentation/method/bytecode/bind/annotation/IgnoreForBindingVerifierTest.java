package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class IgnoreForBindingVerifierTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testIsPresent() throws Exception {
        when(methodDescription.isAnnotationPresent(IgnoreForBinding.class)).thenReturn(true);
        assertThat(IgnoreForBinding.Verifier.check(methodDescription), is(true));
        verify(methodDescription).isAnnotationPresent(IgnoreForBinding.class);
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testIsNotPresent() throws Exception {
        assertThat(IgnoreForBinding.Verifier.check(methodDescription), is(false));
        verify(methodDescription).isAnnotationPresent(IgnoreForBinding.class);
        verifyNoMoreInteractions(methodDescription);
    }
}
