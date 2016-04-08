package net.bytebuddy.agent;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ByteBuddyAgentAgentProviderTest {

    private static final String FOO = "foo";

    @Test
    public void testKnownAgent() throws Exception {
        File agent = mock(File.class);
        when(agent.getAbsolutePath()).thenReturn(FOO);
        ByteBuddyAgent.AgentProvider.ForExistingAgent provider = new ByteBuddyAgent.AgentProvider.ForExistingAgent(agent);
        assertThat(provider.resolve(), is(agent));
    }

    @Test
    public void testKnownAccessor() throws Exception {
        ByteBuddyAgent.AgentProvider provider = ByteBuddyAgent.AgentProvider.ForByteBuddyAgent.INSTANCE;
        assertThat(provider.resolve().isFile(), is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteBuddyAgent.AgentProvider.ForExistingAgent.class).apply();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AgentProvider.ForByteBuddyAgent.class).apply();
    }
}
