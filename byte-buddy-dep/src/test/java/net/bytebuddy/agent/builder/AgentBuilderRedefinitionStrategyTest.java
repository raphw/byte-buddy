package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.instrument.Instrumentation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentBuilderRedefinitionStrategyTest {

    @Test
    public void testDisabledRedefinitionStrategyIsDisabled() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.DISABLED.isEnabled(), is(false));
    }

    @Test
    public void testRetransformationStrategyIsEnabled() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION.isEnabled(), is(true));
    }

    @Test
    public void testRedefinitionStrategyIsEnabled() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.REDEFINITION.isEnabled(), is(true));
    }

    @Test
    public void testDisabledRedefinitionStrategyIsNotRetransforming() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.DISABLED.isRetransforming(mock(Instrumentation.class)), is(false));
    }

    @Test
    public void testRetransformationStrategyIsRetransforming() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        assertThat(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION.isRetransforming(instrumentation), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRetransformationStrategyNotSupportedThrowsException() throws Exception {
        AgentBuilder.RedefinitionStrategy.RETRANSFORMATION.isRetransforming(mock(Instrumentation.class));
    }

    @Test
    public void testRedefinitionStrategyIsNotRetransforming() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        assertThat(AgentBuilder.RedefinitionStrategy.REDEFINITION.isRetransforming(instrumentation), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRedefinitionStrategyNotSupportedThrowsException() throws Exception {
        AgentBuilder.RedefinitionStrategy.REDEFINITION.isRetransforming(mock(Instrumentation.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.PrependableIterator.class).applyBasic();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.ForRedefinition.class).applyBasic();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.ForRetransformation.class).applyBasic();
    }
}
