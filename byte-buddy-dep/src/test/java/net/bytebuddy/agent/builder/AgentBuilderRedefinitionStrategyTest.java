package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;

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
    public void testPrependableIterator() throws Exception {
        AgentBuilder.RedefinitionStrategy.Collector.PrependableIterator iterator
                = new AgentBuilder.RedefinitionStrategy.Collector.PrependableIterator(Collections.singleton(Collections.<Class<?>>singletonList(Void.class)));
        assertThat(iterator.hasNext(), is(true));
        iterator.prepend(Collections.<List<Class<?>>>emptyList());
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(Collections.<Class<?>>singletonList(Void.class)));
        assertThat(iterator.hasNext(), is(false));
        iterator.prepend(Collections.singleton(Collections.<Class<?>>singletonList(String.class)));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(Collections.<Class<?>>singletonList(String.class)));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.class).apply();
    }
}
