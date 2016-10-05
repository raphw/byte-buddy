package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderRedefinitionStrategyListenerTest {

    @Test
    public void testNoOp() throws Exception {
        AgentBuilder.RedefinitionStrategy.Listener.NoOp.INSTANCE.onBatch(0, Collections.<Class<?>>emptyList(), Collections.<Class<?>>emptyList());
        AgentBuilder.RedefinitionStrategy.Listener.NoOp.INSTANCE.onError(0, Collections.<Class<?>>emptyList(), new Throwable(), Collections.<Class<?>>emptyList());
        AgentBuilder.RedefinitionStrategy.Listener.NoOp.INSTANCE.onComplete(0, Collections.<Class<?>>emptyList(), Collections.<List<Class<?>>, Throwable>emptyMap());
    }

    @Test
    public void testYielding() throws Exception {
        AgentBuilder.RedefinitionStrategy.Listener.Yielding.INSTANCE.onBatch(0, Collections.<Class<?>>emptyList(), Collections.<Class<?>>emptyList());
        AgentBuilder.RedefinitionStrategy.Listener.Yielding.INSTANCE.onBatch(1, Collections.<Class<?>>emptyList(), Collections.<Class<?>>emptyList());
        AgentBuilder.RedefinitionStrategy.Listener.Yielding.INSTANCE.onError(0, Collections.<Class<?>>emptyList(), new Throwable(), Collections.<Class<?>>emptyList());
        AgentBuilder.RedefinitionStrategy.Listener.Yielding.INSTANCE.onComplete(0, Collections.<Class<?>>emptyList(), Collections.<List<Class<?>>, Throwable>emptyMap());
    }

    @Test
    public void testPausing() throws Exception {
        AgentBuilder.RedefinitionStrategy.Listener listener = new AgentBuilder.RedefinitionStrategy.Listener.Pausing(1L);
        listener.onBatch(0, Collections.<Class<?>>emptyList(), Collections.<Class<?>>emptyList());
        listener.onBatch(1, Collections.<Class<?>>emptyList(), Collections.<Class<?>>emptyList());
        listener.onError(0, Collections.<Class<?>>emptyList(), new Throwable(), Collections.<Class<?>>emptyList());
        listener.onComplete(0, Collections.<Class<?>>emptyList(), Collections.<List<Class<?>>, Throwable>emptyMap());
    }

    @Test
    public void testStreamWriting() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.RedefinitionStrategy.Listener listener = new AgentBuilder.RedefinitionStrategy.Listener.StreamWriting(printStream);
        listener.onBatch(0, Collections.<Class<?>>emptyList(), Collections.<Class<?>>emptyList());
        Throwable throwable = mock(Throwable.class);
        listener.onError(0, Collections.<Class<?>>emptyList(), throwable, Collections.<Class<?>>emptyList());
        listener.onComplete(0, Collections.<Class<?>>emptyList(), Collections.<List<Class<?>>, Throwable>emptyMap());
        verify(printStream, times(3)).printf(any(String.class), anyInt(), anyInt(), anyInt());
        verifyNoMoreInteractions(printStream);
        verify(throwable).printStackTrace(printStream);
        verifyNoMoreInteractions(throwable);
    }

    @Test
    public void testStreamWritingFactories() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemOut(),
                is((AgentBuilder.RedefinitionStrategy.Listener) new AgentBuilder.RedefinitionStrategy.Listener.StreamWriting(System.out)));
        assertThat(AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemError(),
                is((AgentBuilder.RedefinitionStrategy.Listener) new AgentBuilder.RedefinitionStrategy.Listener.StreamWriting(System.err)));
    }

    @Test
    public void testCompound() throws Exception {
        AgentBuilder.RedefinitionStrategy.Listener first = mock(AgentBuilder.RedefinitionStrategy.Listener.class), second = mock(AgentBuilder.RedefinitionStrategy.Listener.class);
        AgentBuilder.RedefinitionStrategy.Listener listener = new AgentBuilder.RedefinitionStrategy.Listener.Compound(first, second);
        Throwable throwable = new Throwable();
        listener.onBatch(0, Collections.<Class<?>>emptyList(), Collections.<Class<?>>emptyList());
        listener.onError(0, Collections.<Class<?>>emptyList(), throwable, Collections.<Class<?>>emptyList());
        listener.onComplete(0, Collections.<Class<?>>emptyList(), Collections.<List<Class<?>>, Throwable>emptyMap());
        verify(first).onBatch(0, Collections.<Class<?>>emptyList(), Collections.<Class<?>>emptyList());
        verify(first).onError(0, Collections.<Class<?>>emptyList(), throwable, Collections.<Class<?>>emptyList());
        verify(first).onComplete(0, Collections.<Class<?>>emptyList(), Collections.<List<Class<?>>, Throwable>emptyMap());
        verifyNoMoreInteractions(first);
        verify(second).onBatch(0, Collections.<Class<?>>emptyList(), Collections.<Class<?>>emptyList());
        verify(second).onError(0, Collections.<Class<?>>emptyList(), throwable, Collections.<Class<?>>emptyList());
        verify(second).onComplete(0, Collections.<Class<?>>emptyList(), Collections.<List<Class<?>>, Throwable>emptyMap());
        verifyNoMoreInteractions(second);
    }

    @Test(expected = IllegalStateException.class)
    public void testErrorFailFast() throws Exception {
        AgentBuilder.RedefinitionStrategy.Listener.ErrorEscalating.FAIL_FAST.onError(0,
                Collections.<Class<?>>emptyList(),
                mock(Throwable.class),
                Collections.<Class<?>>emptyList());
    }

    @Test
    public void testFailFastNoOp() throws Exception {
        AgentBuilder.RedefinitionStrategy.Listener.ErrorEscalating.FAIL_FAST.onBatch(0, Collections.<Class<?>>emptyList(), Collections.<Class<?>>emptyList());
        AgentBuilder.RedefinitionStrategy.Listener.ErrorEscalating.FAIL_FAST.onComplete(0, Collections.<Class<?>>emptyList(), Collections.<List<Class<?>>, Throwable>emptyMap());
    }

    @Test(expected = IllegalStateException.class)
    public void testErrorFailLast() throws Exception {
        AgentBuilder.RedefinitionStrategy.Listener.ErrorEscalating.FAIL_LAST.onComplete(0,
                Collections.<Class<?>>emptyList(),
                Collections.singletonMap(Collections.<Class<?>>emptyList(), mock(Throwable.class)));
    }

    @Test
    public void testFailLastNoOp() throws Exception {
        AgentBuilder.RedefinitionStrategy.Listener.ErrorEscalating.FAIL_LAST.onBatch(0, Collections.<Class<?>>emptyList(), Collections.<Class<?>>emptyList());
        AgentBuilder.RedefinitionStrategy.Listener.ErrorEscalating.FAIL_LAST.onError(0, Collections.<Class<?>>emptyList(), mock(Throwable.class), Collections.<Class<?>>emptyList());
        AgentBuilder.RedefinitionStrategy.Listener.ErrorEscalating.FAIL_LAST.onComplete(0, Collections.<Class<?>>emptyList(), Collections.<List<Class<?>>, Throwable>emptyMap());
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Listener.NoOp.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Listener.Compound.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Listener.Pausing.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Listener.Yielding.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Listener.ErrorEscalating.class).apply();
    }
}
