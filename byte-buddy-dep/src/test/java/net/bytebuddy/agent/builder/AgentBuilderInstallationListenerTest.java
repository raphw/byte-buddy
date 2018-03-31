package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.PrintStream;
import java.lang.instrument.Instrumentation;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderInstallationListenerTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation instrumentation;

    @Mock
    private ResettableClassFileTransformer classFileTransformer;

    @Mock
    private Throwable throwable;

    @Test
    public void testNoOpListener() throws Exception {
        AgentBuilder.InstallationListener.NoOp.INSTANCE.onBeforeInstall(instrumentation, classFileTransformer);
        AgentBuilder.InstallationListener.NoOp.INSTANCE.onInstall(instrumentation, classFileTransformer);
        assertThat(AgentBuilder.InstallationListener.NoOp.INSTANCE.onError(instrumentation, classFileTransformer, throwable), is(throwable));
        AgentBuilder.InstallationListener.NoOp.INSTANCE.onReset(instrumentation, classFileTransformer);
        verifyZeroInteractions(instrumentation, classFileTransformer, throwable);
    }

    @Test
    public void testPseudoAdapter() throws Exception {
        AgentBuilder.InstallationListener pseudoAdapter = new PseudoAdapter();
        pseudoAdapter.onBeforeInstall(instrumentation, classFileTransformer);
        pseudoAdapter.onInstall(instrumentation, classFileTransformer);
        assertThat(pseudoAdapter.onError(instrumentation, classFileTransformer, throwable), is(throwable));
        pseudoAdapter.onReset(instrumentation, classFileTransformer);
        verifyZeroInteractions(instrumentation, classFileTransformer, throwable);
    }

    @Test
    public void testErrorSuppressing() throws Exception {
        AgentBuilder.InstallationListener.ErrorSuppressing.INSTANCE.onBeforeInstall(instrumentation, classFileTransformer);
        AgentBuilder.InstallationListener.ErrorSuppressing.INSTANCE.onInstall(instrumentation, classFileTransformer);
        AgentBuilder.InstallationListener.NoOp.INSTANCE.onReset(instrumentation, classFileTransformer);
        verifyZeroInteractions(instrumentation, classFileTransformer, throwable);
    }

    @Test
    public void testErrorSuppressingError() throws Exception {
        assertThat(AgentBuilder.InstallationListener.ErrorSuppressing.INSTANCE.onError(instrumentation, classFileTransformer, throwable),
                nullValue(Throwable.class));
    }

    @Test
    public void testStreamWritingListenerBeforeInstall() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.StreamWriting(printStream);
        installationListener.onBeforeInstall(instrumentation, classFileTransformer);
        verify(printStream).printf("[Byte Buddy] BEFORE_INSTALL %s on %s%n", classFileTransformer, instrumentation);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingListenerInstall() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.StreamWriting(printStream);
        installationListener.onInstall(instrumentation, classFileTransformer);
        verify(printStream).printf("[Byte Buddy] INSTALL %s on %s%n", classFileTransformer, instrumentation);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingListenerError() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.StreamWriting(printStream);
        assertThat(installationListener.onError(instrumentation, classFileTransformer, throwable), is(throwable));
        verify(printStream).printf("[Byte Buddy] ERROR %s on %s%n", classFileTransformer, instrumentation);
        verifyNoMoreInteractions(printStream);
        verify(throwable).printStackTrace(printStream);
        verifyNoMoreInteractions(throwable);
    }

    @Test
    public void testStreamWritingListenerReset() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.StreamWriting(printStream);
        installationListener.onReset(instrumentation, classFileTransformer);
        verify(printStream).printf("[Byte Buddy] RESET %s on %s%n", classFileTransformer, instrumentation);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testCompoundListenerBeforeInstall() throws Exception {
        AgentBuilder.InstallationListener first = mock(AgentBuilder.InstallationListener.class), second = mock(AgentBuilder.InstallationListener.class);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.Compound(first, second);
        installationListener.onBeforeInstall(instrumentation, classFileTransformer);
        verify(first).onBeforeInstall(instrumentation, classFileTransformer);
        verify(second).onBeforeInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(first, second);
    }

    @Test
    public void testCompoundListenerInstall() throws Exception {
        AgentBuilder.InstallationListener first = mock(AgentBuilder.InstallationListener.class), second = mock(AgentBuilder.InstallationListener.class);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.Compound(first, second);
        installationListener.onInstall(instrumentation, classFileTransformer);
        verify(first).onInstall(instrumentation, classFileTransformer);
        verify(second).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(first, second);
    }

    @Test
    public void testCompoundListenerError() throws Exception {
        AgentBuilder.InstallationListener first = mock(AgentBuilder.InstallationListener.class), second = mock(AgentBuilder.InstallationListener.class);
        when(first.onError(instrumentation, classFileTransformer, throwable)).thenReturn(throwable);
        when(second.onError(instrumentation, classFileTransformer, throwable)).thenReturn(throwable);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.Compound(first, second);
        assertThat(installationListener.onError(instrumentation, classFileTransformer, throwable), is(throwable));
        verify(first).onError(instrumentation, classFileTransformer, throwable);
        verify(second).onError(instrumentation, classFileTransformer, throwable);
        verifyNoMoreInteractions(first, second);
    }

    @Test
    public void testCompoundListenerErrorHandled() throws Exception {
        AgentBuilder.InstallationListener first = mock(AgentBuilder.InstallationListener.class), second = mock(AgentBuilder.InstallationListener.class);
        when(first.onError(instrumentation, classFileTransformer, throwable)).thenReturn(null);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.Compound(first, second);
        assertThat(installationListener.onError(instrumentation, classFileTransformer, throwable), nullValue(Throwable.class));
        verify(first).onError(instrumentation, classFileTransformer, throwable);
        verifyNoMoreInteractions(first);
        verifyZeroInteractions(second);
    }

    @Test
    public void testCompoundListenerReset() throws Exception {
        AgentBuilder.InstallationListener first = mock(AgentBuilder.InstallationListener.class), second = mock(AgentBuilder.InstallationListener.class);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.Compound(first, second);
        installationListener.onReset(instrumentation, classFileTransformer);
        verify(first).onReset(instrumentation, classFileTransformer);
        verify(second).onReset(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(first, second);
    }

    @Test
    public void testStreamWritingToSystem() throws Exception {
        assertThat(AgentBuilder.InstallationListener.StreamWriting.toSystemOut(),
                hasPrototype((AgentBuilder.InstallationListener) new AgentBuilder.InstallationListener.StreamWriting(System.out)));
        assertThat(AgentBuilder.InstallationListener.StreamWriting.toSystemError(),
                hasPrototype((AgentBuilder.InstallationListener) new AgentBuilder.InstallationListener.StreamWriting(System.err)));
    }

    private static class PseudoAdapter extends AgentBuilder.InstallationListener.Adapter {
        /* empty */
    }
}
