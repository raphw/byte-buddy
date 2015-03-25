package net.bytebuddy.agent;

import net.bytebuddy.test.utility.ToolsJarRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.instrument.Instrumentation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyAgentOpenJdkTest {

    @Rule
    public MethodRule toolsJarRule = new ToolsJarRule();

    @Test
    @ToolsJarRule.Enforce
    public void testAgentInstallation() throws Exception {
        assertThat(ByteBuddyAgent.installOnOpenJDK(), instanceOf(Instrumentation.class));
    }
}
