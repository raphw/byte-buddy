package net.bytebuddy.agent;

import net.bytebuddy.utility.ToolsJarRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class ByteBuddyAgentTest {

    @Rule
    public MethodRule hotSpotRule = new ToolsJarRule();

    @Test
    @ToolsJarRule.Enforce
    public void testAgentInstallation() throws Exception {
        assertThat(ByteBuddyAgent.installOnOpenJDK(), notNullValue());
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
