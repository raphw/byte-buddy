package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AgentBuilderRedefinitionStrategyFailureHandlerTest {

    @Test(expected = IllegalStateException.class)
    public void testFailFastBatch() throws Exception {
        AgentBuilder.RedefinitionStrategy.FailureHandler.Default.FAIL_FAST.onBatchFailure(Collections.<Class<?>>singletonList(Object.class), new Throwable());
    }

    @Test(expected = IllegalStateException.class)
    public void testFailFast() throws Exception {
        AgentBuilder.RedefinitionStrategy.FailureHandler.Default.FAIL_FAST.onFailure(Collections.singletonMap(Collections.<Class<?>>singletonList(Object.class), new Throwable()));
    }

    @Test
    public void testFailLastBatch() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.FailureHandler.Default.FAIL_LAST.onBatchFailure(Collections.<Class<?>>singletonList(Object.class), new Throwable()), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testFailLast() throws Exception {
        AgentBuilder.RedefinitionStrategy.FailureHandler.Default.FAIL_LAST.onFailure(Collections.singletonMap(Collections.<Class<?>>singletonList(Object.class), new Throwable()));
    }

    @Test
    public void testIgnoringBatch() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.FailureHandler.Default.IGNORING.onBatchFailure(Collections.<Class<?>>singletonList(Object.class), new Throwable()), is(false));
    }

    @Test
    public void testIgnoring() throws Exception {
        AgentBuilder.RedefinitionStrategy.FailureHandler.Default.IGNORING.onFailure(Collections.singletonMap(Collections.<Class<?>>singletonList(Object.class), new Throwable()));
    }

    @Test
    public void testSuppressingBatch() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.FailureHandler.Default.SUPPRESSING.onBatchFailure(Collections.<Class<?>>singletonList(Object.class), new Throwable()), is(true));
    }

    @Test
    public void testSuppressing() throws Exception {
        AgentBuilder.RedefinitionStrategy.FailureHandler.Default.SUPPRESSING.onFailure(Collections.singletonMap(Collections.<Class<?>>singletonList(Object.class), new Throwable()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.FailureHandler.Default.class).apply();
    }
}
