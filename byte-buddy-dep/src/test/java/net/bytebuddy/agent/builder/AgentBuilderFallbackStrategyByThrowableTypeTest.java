package net.bytebuddy.agent.builder;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AgentBuilderFallbackStrategyByThrowableTypeTest {

    @Test
    @SuppressWarnings("unchecked") // In absence of @SafeVarargs
    public void testIsFallback() throws Exception {
        assertThat(new AgentBuilder.FallbackStrategy.ByThrowableType(Exception.class).isFallback(Object.class, new Exception()), is(true));
    }

    @Test
    @SuppressWarnings("unchecked") // In absence of @SafeVarargs
    public void testIsFallbackInherited() throws Exception {
        assertThat(new AgentBuilder.FallbackStrategy.ByThrowableType(Exception.class).isFallback(Object.class, new RuntimeException()), is(true));
    }

    @Test
    @SuppressWarnings("unchecked") // In absence of @SafeVarargs
    public void testIsNoFallback() throws Exception {
        assertThat(new AgentBuilder.FallbackStrategy.ByThrowableType(RuntimeException.class).isFallback(Object.class, new Exception()), is(false));
    }
}
