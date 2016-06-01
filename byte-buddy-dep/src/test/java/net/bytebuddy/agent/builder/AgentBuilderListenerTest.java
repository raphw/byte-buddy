package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.JavaModule;
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
    private TypeDescription typeDescription;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private JavaModule module;

    @Mock
    private DynamicType dynamicType;

    @Mock
    private Throwable throwable;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.getName()).thenReturn(FOO);
    }

    @Test
    public void testNoOp() throws Exception {
        AgentBuilder.Listener.NoOp.INSTANCE.onTransformation(typeDescription, classLoader, module, dynamicType);
        verifyZeroInteractions(dynamicType);
        AgentBuilder.Listener.NoOp.INSTANCE.onError(FOO, classLoader, module, throwable);
        verifyZeroInteractions(throwable);
        AgentBuilder.Listener.NoOp.INSTANCE.onIgnored(typeDescription, classLoader, module);
        AgentBuilder.Listener.NoOp.INSTANCE.onComplete(FOO, classLoader, module);
    }

    @Test
    public void testPseudoAdapter() throws Exception {
        AgentBuilder.Listener listener = new PseudoAdapter();
        listener.onTransformation(typeDescription, classLoader, module, dynamicType);
        verifyZeroInteractions(dynamicType);
        listener.onError(FOO, classLoader, module, throwable);
        verifyZeroInteractions(throwable);
        listener.onIgnored(typeDescription, classLoader, module);
        listener.onComplete(FOO, classLoader, module);
    }

    @Test
    public void testCompoundOnTransformation() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onTransformation(typeDescription, classLoader, module, dynamicType);
        verify(first).onTransformation(typeDescription, classLoader, module, dynamicType);
        verifyNoMoreInteractions(first);
        verify(second).onTransformation(typeDescription, classLoader, module, dynamicType);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundOnError() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onError(FOO, classLoader, module, throwable);
        verify(first).onError(FOO, classLoader, module, throwable);
        verifyNoMoreInteractions(first);
        verify(second).onError(FOO, classLoader, module, throwable);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundOnIgnored() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onIgnored(typeDescription, classLoader, module);
        verify(first).onIgnored(typeDescription, classLoader, module);
        verifyNoMoreInteractions(first);
        verify(second).onIgnored(typeDescription, classLoader, module);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundOnComplete() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onComplete(FOO, classLoader, module);
        verify(first).onComplete(FOO, classLoader, module);
        verifyNoMoreInteractions(first);
        verify(second).onComplete(FOO, classLoader, module);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testStreamWritingOnTransformation() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onTransformation(typeDescription, classLoader, module, dynamicType);
        verify(printStream).println("[Byte Buddy] TRANSFORM " + FOO + "[" + classLoader + ", " + module + "]");
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingOnError() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onError(FOO, classLoader, module, throwable);
        verify(printStream).println("[Byte Buddy] ERROR " + FOO + "[" + classLoader + ", " + module + "]");
        verifyNoMoreInteractions(printStream);
        verify(throwable).printStackTrace(printStream);
        verifyNoMoreInteractions(throwable);
    }

    @Test
    public void testStreamWritingOnComplete() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onComplete(FOO, classLoader, module);
        verify(printStream).println("[Byte Buddy] COMPLETE " + FOO + "[" + classLoader + ", " + module + "]");
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingOnIgnore() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onIgnored(typeDescription, classLoader, module);
        verify(printStream).println("[Byte Buddy] IGNORE " + FOO + "[" + classLoader + ", " + module + "]");
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
