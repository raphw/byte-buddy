package net.bytebuddy.agent.builder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.util.Collections;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderInstallationListenerTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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
        AgentBuilder.InstallationListener.NoOp.INSTANCE.onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        AgentBuilder.InstallationListener.NoOp.INSTANCE.onWarmUpError(Object.class, classFileTransformer, throwable);
        AgentBuilder.InstallationListener.NoOp.INSTANCE.onAfterWarmUp(Collections.<Class<?>, byte[]>singletonMap(Object.class, null), classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation, classFileTransformer, throwable);
    }

    @Test
    public void testPseudoAdapter() throws Exception {
        AgentBuilder.InstallationListener pseudoAdapter = new PseudoAdapter();
        pseudoAdapter.onBeforeInstall(instrumentation, classFileTransformer);
        pseudoAdapter.onInstall(instrumentation, classFileTransformer);
        assertThat(pseudoAdapter.onError(instrumentation, classFileTransformer, throwable), is(throwable));
        pseudoAdapter.onReset(instrumentation, classFileTransformer);
        pseudoAdapter.onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        pseudoAdapter.onWarmUpError(Object.class, classFileTransformer, throwable);
        pseudoAdapter.onAfterWarmUp(Collections.<Class<?>, byte[]>singletonMap(Object.class, null), classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation, classFileTransformer, throwable);
    }

    @Test
    public void testErrorSuppressing() throws Exception {
        AgentBuilder.InstallationListener.ErrorSuppressing.INSTANCE.onBeforeInstall(instrumentation, classFileTransformer);
        AgentBuilder.InstallationListener.ErrorSuppressing.INSTANCE.onInstall(instrumentation, classFileTransformer);
        AgentBuilder.InstallationListener.NoOp.INSTANCE.onReset(instrumentation, classFileTransformer);
        AgentBuilder.InstallationListener.NoOp.INSTANCE.onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        AgentBuilder.InstallationListener.NoOp.INSTANCE.onWarmUpError(Object.class, classFileTransformer, throwable);
        AgentBuilder.InstallationListener.NoOp.INSTANCE.onAfterWarmUp(Collections.<Class<?>, byte[]>singletonMap(Object.class, null), classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation, classFileTransformer, throwable);
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
    public void testStreamWritingListenerBeforeWarmUp() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.StreamWriting(printStream);
        installationListener.onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        verify(printStream).printf("[Byte Buddy] BEFORE_WARMUP %s on %s%n", classFileTransformer, Collections.singleton(Object.class));
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingListenerWarmUpError() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.StreamWriting(printStream);
        installationListener.onWarmUpError(Object.class, classFileTransformer, throwable);
        verify(printStream).printf("[Byte Buddy] ERROR_WARMUP %s on %s%n", classFileTransformer, Object.class);
        verifyNoMoreInteractions(printStream);
        verify(throwable).printStackTrace(printStream);
        verifyNoMoreInteractions(throwable);
    }

    @Test
    public void testStreamWritingListenerAfterWarmUp() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.StreamWriting(printStream);
        installationListener.onAfterWarmUp(Collections.<Class<?>, byte[]>singletonMap(Object.class, null), classFileTransformer, true);
        verify(printStream).printf("[Byte Buddy] AFTER_WARMUP %s %s on %s%n", "transformed", classFileTransformer, Collections.singleton(Object.class));
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
        verifyNoMoreInteractions(second);
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
    public void testCompoundListenerBeforeWarmUp() throws Exception {
        AgentBuilder.InstallationListener first = mock(AgentBuilder.InstallationListener.class), second = mock(AgentBuilder.InstallationListener.class);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.Compound(first, second);
        installationListener.onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        verify(first).onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        verify(second).onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        verifyNoMoreInteractions(first, second);
    }

    @Test
    public void testCompoundListenerWarmUpError() throws Exception {
        AgentBuilder.InstallationListener first = mock(AgentBuilder.InstallationListener.class), second = mock(AgentBuilder.InstallationListener.class);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.Compound(first, second);
        installationListener.onWarmUpError(Object.class, classFileTransformer, throwable);
        verify(first).onWarmUpError(Object.class, classFileTransformer, throwable);
        verify(second).onWarmUpError(Object.class, classFileTransformer, throwable);
        verifyNoMoreInteractions(first, second);
    }

    @Test
    public void testCompoundListenerAfterWarmUp() throws Exception {
        AgentBuilder.InstallationListener first = mock(AgentBuilder.InstallationListener.class), second = mock(AgentBuilder.InstallationListener.class);
        AgentBuilder.InstallationListener installationListener = new AgentBuilder.InstallationListener.Compound(first, second);
        installationListener.onAfterWarmUp(Collections.<Class<?>, byte[]>singletonMap(Object.class, null), classFileTransformer, true);
        verify(first).onAfterWarmUp(Collections.<Class<?>, byte[]>singletonMap(Object.class, null), classFileTransformer, true);
        verify(second).onAfterWarmUp(Collections.<Class<?>, byte[]>singletonMap(Object.class, null), classFileTransformer, true);
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
