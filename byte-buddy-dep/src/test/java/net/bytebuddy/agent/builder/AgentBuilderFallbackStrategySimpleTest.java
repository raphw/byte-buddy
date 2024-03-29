package net.bytebuddy.agent.builder;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AgentBuilderFallbackStrategySimpleTest {

    @Test
    public void testEnabled() throws Exception {
        assertThat(AgentBuilder.FallbackStrategy.Simple.ENABLED.isFallback(Object.class, new Throwable()), is(true));
    }

    @Test
    public void testDisabled() throws Exception {
        assertThat(AgentBuilder.FallbackStrategy.Simple.DISABLED.isFallback(Object.class, new Throwable()), is(false));
    }
}
