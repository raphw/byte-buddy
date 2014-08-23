package net.bytebuddy.agent;

import net.bytebuddy.utility.ToolsJarRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyAgentTest {

    @Rule
    public MethodRule hotSpotRule = new ToolsJarRule();

    @Test
    @ToolsJarRule.Enforce
    public void testAgentInstallation() throws Exception {
        assertThat(ByteBuddyAgent.installOnOpenJDK(), notNullValue());
    }
}
