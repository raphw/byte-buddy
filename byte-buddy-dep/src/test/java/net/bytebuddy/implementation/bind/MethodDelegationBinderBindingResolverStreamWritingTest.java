package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.PrintStream;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MethodDelegationBinderBindingResolverStreamWritingTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription source, target;

    @Mock
    private MethodDelegationBinder.MethodBinding methodBinding;

    @Mock
    private MethodDelegationBinder.AmbiguityResolver ambiguityResolver;

    @Before
    public void setUp() throws Exception {
        when(methodBinding.getTarget()).thenReturn(target);
    }

    @Test
    public void testStreamWriting() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        MethodDelegationBinder.BindingResolver delegate = mock(MethodDelegationBinder.BindingResolver.class);
        when(delegate.resolve(ambiguityResolver, source, Collections.singletonList(methodBinding))).thenReturn(methodBinding);
        assertThat(new MethodDelegationBinder.BindingResolver.StreamWriting(delegate, printStream).resolve(ambiguityResolver,
                source,
                Collections.singletonList(methodBinding)), is(methodBinding));
        verify(printStream).println("Binding " + source + " as delegation to " + target);
    }

    @Test
    public void testSystemOut() throws Exception {
        assertThat(MethodDelegationBinder.BindingResolver.StreamWriting.toSystemOut(), is((MethodDelegationBinder.BindingResolver)
                new MethodDelegationBinder.BindingResolver.StreamWriting(MethodDelegationBinder.BindingResolver.Default.INSTANCE, System.out)));
    }

    @Test
    public void testSystemError() throws Exception {
        assertThat(MethodDelegationBinder.BindingResolver.StreamWriting.toSystemError(), is((MethodDelegationBinder.BindingResolver)
                new MethodDelegationBinder.BindingResolver.StreamWriting(MethodDelegationBinder.BindingResolver.Default.INSTANCE, System.err)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodDelegationBinder.BindingResolver.StreamWriting.class).apply();
    }
}
