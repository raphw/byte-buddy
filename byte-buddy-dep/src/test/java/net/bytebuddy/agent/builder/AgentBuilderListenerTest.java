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
    private TypeDescription typeDescription;

    @Mock
    private Throwable throwable;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.getName()).thenReturn(FOO);
    }

    @Test
    public void testNoOp() throws Exception {
        AgentBuilder.Listener.NoOp.INSTANCE.onTransformation(typeDescription, dynamicType);
        verifyZeroInteractions(dynamicType);
        AgentBuilder.Listener.NoOp.INSTANCE.onError(FOO, throwable);
        verifyZeroInteractions(throwable);
        AgentBuilder.Listener.NoOp.INSTANCE.onIgnored(typeDescription);
        AgentBuilder.Listener.NoOp.INSTANCE.onComplete(FOO);
    }

    @Test
    public void testCompoundOnTransformation() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onTransformation(typeDescription, dynamicType);
        verify(first).onTransformation(typeDescription, dynamicType);
        verifyNoMoreInteractions(first);
        verify(second).onTransformation(typeDescription, dynamicType);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundOnError() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onError(FOO, throwable);
        verify(first).onError(FOO, throwable);
        verifyNoMoreInteractions(first);
        verify(second).onError(FOO, throwable);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundOnIgnored() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onIgnored(typeDescription);
        verify(first).onIgnored(typeDescription);
        verifyNoMoreInteractions(first);
        verify(second).onIgnored(typeDescription);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundOnComplete() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onComplete(FOO);
        verify(first).onComplete(FOO);
        verifyNoMoreInteractions(first);
        verify(second).onComplete(FOO);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testStreamWritingOnTransformation() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onTransformation(typeDescription, dynamicType);
        verify(printStream).println("[Byte Buddy] TRANSFORM " + FOO);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingOnError() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onError(FOO, throwable);
        verify(printStream).println("[Byte Buddy] ERROR " + FOO);
        verifyNoMoreInteractions(printStream);
        verify(throwable).printStackTrace(printStream);
        verifyNoMoreInteractions(throwable);
    }

    @Test
    public void testStreamWritingOnComplete() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onComplete(FOO);
        verify(printStream).println("[Byte Buddy] COMPLETE " + FOO);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingOnIgnore() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onIgnored(typeDescription);
        verify(printStream).println("[Byte Buddy] IGNORE " + FOO);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.Listener.NoOp.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Listener.StreamWriting.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Listener.Compound.class).apply();
    }
}
