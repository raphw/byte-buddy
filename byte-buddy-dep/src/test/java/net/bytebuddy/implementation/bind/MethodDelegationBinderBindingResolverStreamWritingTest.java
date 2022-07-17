package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.io.PrintStream;
import java.util.Collections;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodDelegationBinderBindingResolverStreamWritingTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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
        assertThat(MethodDelegationBinder.BindingResolver.StreamWriting.toSystemOut(), hasPrototype((MethodDelegationBinder.BindingResolver)
                new MethodDelegationBinder.BindingResolver.StreamWriting(MethodDelegationBinder.BindingResolver.Default.INSTANCE, System.out)));
    }

    @Test
    public void testSystemError() throws Exception {
        assertThat(MethodDelegationBinder.BindingResolver.StreamWriting.toSystemError(), hasPrototype((MethodDelegationBinder.BindingResolver)
                new MethodDelegationBinder.BindingResolver.StreamWriting(MethodDelegationBinder.BindingResolver.Default.INSTANCE, System.err)));
    }
}
