package net.bytebuddy.agent;

import net.bytebuddy.utility.ToolsJarRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class ByteBuddyAgentTest {

    private static final String INSTRUMENTATION = "instrumentation";

    private static final Object STATIC_FIELD = null;

    @Rule
    public MethodRule hotSpotRule = new ToolsJarRule();

    @After
    public void tearDown() throws Exception {
        Field field = ByteBuddyAgent.Installer.class.getDeclaredField(INSTRUMENTATION);
        field.setAccessible(true);
        field.set(STATIC_FIELD, null);
    }

    @Test
    @ToolsJarRule.Enforce
    public void testAgentInstallation() throws Exception {
        assertThat(ByteBuddyAgent.installOnOpenJDK(), notNullValue());
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
        ByteBuddyAgent.getInstrumentation();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConstructorThrowsException() throws Exception {
        Constructor<?> constructor = ByteBuddyAgent.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }
}
