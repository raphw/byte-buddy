package net.bytebuddy.agent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class ByteBuddyAgentTest {

    private static final String INSTRUMENTATION = "instrumentation";

    private static final Object STATIC_FIELD = null;

    private Instrumentation actualInstrumentation;

    @Before
    public void setUp() throws Exception {
        Field field = ByteBuddyAgent.Installer.class.getDeclaredField(INSTRUMENTATION);
        field.setAccessible(true);
        actualInstrumentation = (Instrumentation) field.get(STATIC_FIELD);
    }

    @After
    public void tearDown() throws Exception {
        Field field = ByteBuddyAgent.Installer.class.getDeclaredField(INSTRUMENTATION);
        field.setAccessible(true);
        field.set(STATIC_FIELD, actualInstrumentation);
    }

    @Test
    public void testInstrumentationExtraction() throws Exception {
        Field field = ByteBuddyAgent.Installer.class.getDeclaredField(INSTRUMENTATION);
        field.setAccessible(true);
        Instrumentation instrumentation = mock(Instrumentation.class);
        field.set(STATIC_FIELD, instrumentation);
        assertThat(ByteBuddyAgent.getInstrumentation(), is(instrumentation));
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingInstrumentationThrowsException() throws Exception {
        Field field = ByteBuddyAgent.Installer.class.getDeclaredField(INSTRUMENTATION);
        field.setAccessible(true);
        field.set(STATIC_FIELD, null);
        ByteBuddyAgent.getInstrumentation();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConstructorThrowsException() throws Exception {
        Constructor<?> constructor = ByteBuddyAgent.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            throw (Exception) exception.getCause();
        }
    }
}
