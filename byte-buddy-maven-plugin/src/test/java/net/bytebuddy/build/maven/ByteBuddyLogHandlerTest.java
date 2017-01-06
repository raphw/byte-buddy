package net.bytebuddy.build.maven;

import net.bytebuddy.test.utility.MockitoRule;
import org.apache.maven.plugin.logging.Log;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

public class ByteBuddyLogHandlerTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Log log;

    @Test
    public void testLogPublishNoDebug() throws Exception {
        ByteBuddyLogHandler byteBuddyLogHandler = new ByteBuddyLogHandler(log, mock(Logger.class), false);
        LogRecord logRecord = new LogRecord(Level.INFO, FOO);
        byteBuddyLogHandler.publish(logRecord);
        verify(log).isDebugEnabled();
        verifyNoMoreInteractions(log);
    }

    @Test
    public void testLogPublishDebug() throws Exception {
        ByteBuddyLogHandler byteBuddyLogHandler = new ByteBuddyLogHandler(log, mock(Logger.class), false);
        LogRecord logRecord = new LogRecord(Level.INFO, FOO);
        when(log.isDebugEnabled()).thenReturn(true);
        byteBuddyLogHandler.publish(logRecord);
        verify(log).isDebugEnabled();
        verify(log).debug(byteBuddyLogHandler.getFormatter().format(logRecord));
        verifyNoMoreInteractions(log);
    }

    @Test
    public void testFlush() throws Exception {
        new ByteBuddyLogHandler(log, mock(Logger.class), false).flush();
    }

    @Test
    public void testClose() throws Exception {
        new ByteBuddyLogHandler(log, mock(Logger.class), false).close();
    }

    @Test
    public void testInitialization() throws Exception {
        ByteBuddyLogHandler.initialize(log).reset();
    }
}
