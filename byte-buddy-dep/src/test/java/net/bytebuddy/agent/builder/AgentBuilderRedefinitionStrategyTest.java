package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.instrument.Instrumentation;

import static org.hamcrest.CoreMatchers.instanceOf;
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
    public void testDisabledRedefinitionStrategyIsRetransforming() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.DISABLED.isRetransforming(), is(false));
    }

    @Test
    public void testRetransformationStrategyIsRetransforming() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION.isRetransforming(), is(true));
    }

    @Test
    public void testRedefinitionStrategyIsRetransforming() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.REDEFINITION.isRetransforming(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testDisabledRedefinitionStrategyIsNotChecked() throws Exception {
        AgentBuilder.RedefinitionStrategy.DISABLED.check(mock(Instrumentation.class));
    }

    @Test
    public void testRetransformationStrategyIsChecked() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        AgentBuilder.RedefinitionStrategy.RETRANSFORMATION.check(instrumentation);
    }

    @Test(expected = IllegalStateException.class)
    public void testRetransformationStrategyNotSupportedThrowsException() throws Exception {
        AgentBuilder.RedefinitionStrategy.RETRANSFORMATION.check(mock(Instrumentation.class));
    }

    @Test
    public void testRedefinitionStrategyIsChecked() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        AgentBuilder.RedefinitionStrategy.REDEFINITION.check(instrumentation);
    }

    @Test(expected = IllegalStateException.class)
    public void testRedefinitionStrategyNotSupportedThrowsException() throws Exception {
        AgentBuilder.RedefinitionStrategy.REDEFINITION.check(mock(Instrumentation.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.PrependableIterator.class).applyBasic();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.ForRedefinition.class).applyBasic();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.ForRetransformation.class).applyBasic();
    }
}
