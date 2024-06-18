package net.bytebuddy.agent;

import org.hamcrest.CoreMatchers;
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

    private Instrumentation actualInstrumentation;

    @Before
    public void setUp() throws Exception {
        Field field = Installer.class.getDeclaredField("instrumentation");
        field.setAccessible(true);
        actualInstrumentation = (Instrumentation) field.get(null);
    }

    @After
    public void tearDown() throws Exception {
        Field field = Installer.class.getDeclaredField("instrumentation");
        field.setAccessible(true);
        field.set(null, actualInstrumentation);
    }

    @Test
    public void testInstrumentationExtraction() throws Exception {
        Field field = Installer.class.getDeclaredField("instrumentation");
        field.setAccessible(true);
        Instrumentation instrumentation = mock(Instrumentation.class);
        field.set(null, instrumentation);
        assertThat(ByteBuddyAgent.getInstrumentation(), is(instrumentation));
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingInstrumentationThrowsException() throws Exception {
        Field field = Installer.class.getDeclaredField("instrumentation");
        field.setAccessible(true);
        field.set(null, null);
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
            throw (Exception) exception.getTargetException();
        }
    }
}
