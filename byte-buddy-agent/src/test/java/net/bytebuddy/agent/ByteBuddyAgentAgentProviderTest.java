package net.bytebuddy.agent;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ByteBuddyAgentAgentProviderTest {

    private static final String FOO = "foo";

    @Test
    public void testKnownAgent() throws Exception {
        File agent = mock(File.class);
        when(agent.getAbsolutePath()).thenReturn(FOO);
        ByteBuddyAgent.AgentProvider.ForExistingAgent provider = new ByteBuddyAgent.AgentProvider.ForExistingAgent(agent);
        assertThat(provider.getAbsolutePath(), is(FOO));
        verify(agent).getAbsolutePath();
        verifyNoMoreInteractions(agent);
        assertThat(provider.provide(), is((ByteBuddyAgent.AgentProvider.Agent) provider));
        provider.release();
        verifyNoMoreInteractions(agent);
    }

    @Test
    public void testKnownAccessor() throws Exception {
        ByteBuddyAgent.AgentProvider provider = ByteBuddyAgent.AgentProvider.ForByteBuddyAgent.INSTANCE;
        ByteBuddyAgent.AgentProvider.Agent agent = provider.provide();
        assertThat(new File(agent.getAbsolutePath()).exists(), is(true));
        assertThat(agent.getAbsolutePath(), notNullValue(String.class));
        agent.release();
        assertThat(new File(agent.getAbsolutePath()).exists(), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteBuddyAgent.AgentProvider.ForExistingAgent.class).apply();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AgentProvider.ForByteBuddyAgent.class).apply();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AgentProvider.Agent.ForTemporaryAgent.class).apply();
    }

}
