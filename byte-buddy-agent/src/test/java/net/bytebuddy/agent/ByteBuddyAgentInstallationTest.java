package net.bytebuddy.agent;

import net.bytebuddy.test.utility.AgentAttachmentRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.instrument.Instrumentation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyAgentInstallationTest {

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Test
    @AgentAttachmentRule.Enforce
    public void testAgentInstallation() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
    }
}
