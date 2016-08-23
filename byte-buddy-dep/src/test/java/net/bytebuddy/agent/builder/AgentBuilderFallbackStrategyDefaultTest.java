package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AgentBuilderFallbackStrategyDefaultTest {

    @Test
    public void testEnabled() throws Exception {
        assertThat(AgentBuilder.FallbackStrategy.Default.ENABLED.isFallback(Object.class, new Throwable()), is(true));
    }

    @Test
    public void testDisabled() throws Exception {
        assertThat(AgentBuilder.FallbackStrategy.Default.DISABLED.isFallback(Object.class, new Throwable()), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.FallbackStrategy.Default.class).apply();
    }
}
