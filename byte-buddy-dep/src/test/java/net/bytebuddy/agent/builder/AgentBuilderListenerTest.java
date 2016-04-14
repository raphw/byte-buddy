package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.PrintStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderListenerTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private DynamicType.Builder<?> builder;

    @Mock
    private AgentBuilder.Listener first, second;

    @Mock
    private DynamicType dynamicType;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private Throwable throwable;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.getName()).thenReturn(FOO);
    }

    @Test
    public void testNoOp() throws Exception {
        AgentBuilder.Listener.NoOp.INSTANCE.onTransformation(typeDescription, classLoader, dynamicType);
        verifyZeroInteractions(dynamicType);
        AgentBuilder.Listener.NoOp.INSTANCE.onError(FOO, classLoader, throwable);
        verifyZeroInteractions(throwable);
        AgentBuilder.Listener.NoOp.INSTANCE.onIgnored(typeDescription, classLoader);
        AgentBuilder.Listener.NoOp.INSTANCE.onComplete(FOO, classLoader);
    }

    @Test
    public void testPseudoAdapter() throws Exception {
        AgentBuilder.Listener listener = new PseudoAdapter();
        listener.onTransformation(typeDescription, classLoader, dynamicType);
        verifyZeroInteractions(dynamicType);
        listener.onError(FOO, classLoader, throwable);
        verifyZeroInteractions(throwable);
        listener.onIgnored(typeDescription, classLoader);
        listener.onComplete(FOO, classLoader);
    }

    @Test
    public void testCompoundOnTransformation() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onTransformation(typeDescription, classLoader, dynamicType);
        verify(first).onTransformation(typeDescription, classLoader, dynamicType);
        verifyNoMoreInteractions(first);
        verify(second).onTransformation(typeDescription, classLoader, dynamicType);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundOnError() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onError(FOO, classLoader, throwable);
        verify(first).onError(FOO, classLoader, throwable);
        verifyNoMoreInteractions(first);
        verify(second).onError(FOO, classLoader, throwable);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundOnIgnored() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onIgnored(typeDescription, classLoader);
        verify(first).onIgnored(typeDescription, classLoader);
        verifyNoMoreInteractions(first);
        verify(second).onIgnored(typeDescription, classLoader);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundOnComplete() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onComplete(FOO, classLoader);
        verify(first).onComplete(FOO, classLoader);
        verifyNoMoreInteractions(first);
        verify(second).onComplete(FOO, classLoader);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testStreamWritingOnTransformation() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onTransformation(typeDescription, classLoader, dynamicType);
        verify(printStream).println("[Byte Buddy] TRANSFORM " + FOO);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingOnError() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onError(FOO, classLoader, throwable);
        verify(printStream).println("[Byte Buddy] ERROR " + FOO);
        verifyNoMoreInteractions(printStream);
        verify(throwable).printStackTrace(printStream);
        verifyNoMoreInteractions(throwable);
    }

    @Test
    public void testStreamWritingOnComplete() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onComplete(FOO, classLoader);
        verify(printStream).println("[Byte Buddy] COMPLETE " + FOO);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingOnIgnore() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onIgnored(typeDescription, classLoader);
        verify(printStream).println("[Byte Buddy] IGNORE " + FOO);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingStandardOutput() throws Exception {
        assertThat(AgentBuilder.Listener.StreamWriting.toSystemOut(), is((AgentBuilder.Listener) new AgentBuilder.Listener.StreamWriting(System.out)));
    }

    @Test
    public void testStreamWritingStandardError() throws Exception {
        assertThat(AgentBuilder.Listener.StreamWriting.toSystemError(), is((AgentBuilder.Listener) new AgentBuilder.Listener.StreamWriting(System.err)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.Listener.NoOp.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Listener.StreamWriting.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Listener.Compound.class).apply();
    }

    private static class PseudoAdapter extends AgentBuilder.Listener.Adapter {
        /* empty */
    }
}
