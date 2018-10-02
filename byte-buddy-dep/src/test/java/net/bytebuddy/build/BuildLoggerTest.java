package net.bytebuddy.build;

import org.junit.Test;

import java.io.PrintStream;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class BuildLoggerTest {

    private static final String FOO = "foo";

    @Test
    public void testNonOperational() {
        assertThat(BuildLogger.NoOp.INSTANCE.isDebugEnabled(), is(false));
        assertThat(BuildLogger.NoOp.INSTANCE.isInfoEnabled(), is(false));
        assertThat(BuildLogger.NoOp.INSTANCE.isWarnEnabled(), is(false));
        assertThat(BuildLogger.NoOp.INSTANCE.isErrorEnabled(), is(false));
        BuildLogger.NoOp.INSTANCE.debug(FOO);
        BuildLogger.NoOp.INSTANCE.debug(FOO, new RuntimeException());
        BuildLogger.NoOp.INSTANCE.info(FOO);
        BuildLogger.NoOp.INSTANCE.info(FOO, new RuntimeException());
        BuildLogger.NoOp.INSTANCE.warn(FOO);
        BuildLogger.NoOp.INSTANCE.warn(FOO, new RuntimeException());
        BuildLogger.NoOp.INSTANCE.error(FOO);
        BuildLogger.NoOp.INSTANCE.error(FOO, new RuntimeException());
    }

    @Test
    public void testAdapterNonOperational() {
        BuildLogger buildLogger = new BuildLogger.Adapter() {
            /* empty */
        };
        assertThat(buildLogger.isDebugEnabled(), is(false));
        assertThat(buildLogger.isInfoEnabled(), is(false));
        assertThat(buildLogger.isWarnEnabled(), is(false));
        assertThat(buildLogger.isErrorEnabled(), is(false));
        buildLogger.debug(FOO);
        buildLogger.debug(FOO, new RuntimeException());
        buildLogger.info(FOO);
        buildLogger.info(FOO, new RuntimeException());
        buildLogger.warn(FOO);
        buildLogger.warn(FOO, new RuntimeException());
        buildLogger.error(FOO);
        buildLogger.error(FOO, new RuntimeException());
    }

    @Test
    public void testStreamWriting() {
        PrintStream printStream = mock(PrintStream.class);
        BuildLogger buildLogger = new BuildLogger.StreamWriting(printStream);
        assertThat(buildLogger.isDebugEnabled(), is(true));
        assertThat(buildLogger.isInfoEnabled(), is(true));
        assertThat(buildLogger.isWarnEnabled(), is(true));
        assertThat(buildLogger.isErrorEnabled(), is(true));
        Throwable throwable = mock(Throwable.class);
        buildLogger.debug(FOO);
        buildLogger.debug(FOO, throwable);
        buildLogger.info(FOO);
        buildLogger.info(FOO, throwable);
        buildLogger.warn(FOO);
        buildLogger.warn(FOO, throwable);
        buildLogger.error(FOO);
        buildLogger.error(FOO, throwable);
        verify(printStream, times(8)).print(FOO);
        verifyNoMoreInteractions(printStream);
        verify(throwable, times(4)).printStackTrace(printStream);
        verifyNoMoreInteractions(throwable);
    }

    @Test
    public void testStreamWritingDefaults() {
        assertThat(BuildLogger.StreamWriting.toSystemOut(), hasPrototype((BuildLogger) new BuildLogger.StreamWriting(System.out)));
        assertThat(BuildLogger.StreamWriting.toSystemError(), hasPrototype((BuildLogger) new BuildLogger.StreamWriting(System.err)));
    }

    @Test
    public void testCompoundInactive() {
        BuildLogger delegate = mock(BuildLogger.class);
        BuildLogger buildLogger = new BuildLogger.Compound(delegate);
        assertThat(buildLogger.isDebugEnabled(), is(false));
        assertThat(buildLogger.isInfoEnabled(), is(false));
        assertThat(buildLogger.isWarnEnabled(), is(false));
        assertThat(buildLogger.isErrorEnabled(), is(false));
        Throwable throwable = new Throwable();
        buildLogger.debug(FOO);
        buildLogger.debug(FOO, throwable);
        buildLogger.info(FOO);
        buildLogger.info(FOO, throwable);
        buildLogger.warn(FOO);
        buildLogger.warn(FOO, throwable);
        buildLogger.error(FOO);
        buildLogger.error(FOO, throwable);
        verify(delegate, never()).debug(FOO);
        verify(delegate, never()).debug(FOO, throwable);
        verify(delegate, never()).info(FOO);
        verify(delegate, never()).info(FOO, throwable);
        verify(delegate, never()).warn(FOO);
        verify(delegate, never()).warn(FOO, throwable);
        verify(delegate, never()).error(FOO);
        verify(delegate, never()).error(FOO, throwable);
    }

    @Test
    public void testCompoundActive() {
        BuildLogger delegate = mock(BuildLogger.class);
        when(delegate.isDebugEnabled()).thenReturn(true);
        when(delegate.isInfoEnabled()).thenReturn(true);
        when(delegate.isWarnEnabled()).thenReturn(true);
        when(delegate.isErrorEnabled()).thenReturn(true);
        BuildLogger buildLogger = new BuildLogger.Compound(delegate);
        assertThat(buildLogger.isDebugEnabled(), is(true));
        assertThat(buildLogger.isInfoEnabled(), is(true));
        assertThat(buildLogger.isWarnEnabled(), is(true));
        assertThat(buildLogger.isErrorEnabled(), is(true));
        Throwable throwable = new Throwable();
        buildLogger.debug(FOO);
        buildLogger.debug(FOO, throwable);
        buildLogger.info(FOO);
        buildLogger.info(FOO, throwable);
        buildLogger.warn(FOO);
        buildLogger.warn(FOO, throwable);
        buildLogger.error(FOO);
        buildLogger.error(FOO, throwable);
        verify(delegate).debug(FOO);
        verify(delegate).debug(FOO, throwable);
        verify(delegate).info(FOO);
        verify(delegate).info(FOO, throwable);
        verify(delegate).warn(FOO);
        verify(delegate).warn(FOO, throwable);
        verify(delegate).error(FOO);
        verify(delegate).error(FOO, throwable);
    }
}
