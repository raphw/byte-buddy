package net.bytebuddy.build.gradle;

import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.gradle.api.Project;
import org.junit.Before;
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
    private Project project;

    @Mock
    private org.gradle.api.logging.Logger logger;

    @Before
    public void setUp() throws Exception {
        when(project.getLogger()).thenReturn(logger);
    }

    @Test
    public void testLogPublishNoDebug() throws Exception {
        ByteBuddyLogHandler byteBuddyLogHandler = new ByteBuddyLogHandler(project, mock(Logger.class), false);
        LogRecord logRecord = new LogRecord(Level.INFO, FOO);
        byteBuddyLogHandler.publish(logRecord);
        verify(logger).isDebugEnabled();
        verifyNoMoreInteractions(logger);
    }

    @Test
    public void testLogPublishDebug() throws Exception {
        ByteBuddyLogHandler byteBuddyLogHandler = new ByteBuddyLogHandler(project, mock(Logger.class), false);
        LogRecord logRecord = new LogRecord(Level.INFO, FOO);
        when(logger.isDebugEnabled()).thenReturn(true);
        byteBuddyLogHandler.publish(logRecord);
        verify(logger).isDebugEnabled();
        verify(logger).debug(byteBuddyLogHandler.getFormatter().format(logRecord));
        verifyNoMoreInteractions(logger);
    }

    @Test
    public void testFlush() throws Exception {
        new ByteBuddyLogHandler(project, mock(Logger.class), false).flush();
    }

    @Test
    public void testClose() throws Exception {
        new ByteBuddyLogHandler(project, mock(Logger.class), false).close();
    }

    @Test
    public void testInitialization() throws Exception {
        ByteBuddyLogHandler.initialize(project).reset();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteBuddyLogHandler.class).apply();
    }
}
