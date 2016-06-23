package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.Iterator;

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
    public void testRetransformationChunkedStrategyIsEnabled() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION_CHUNKED.isEnabled(), is(true));
    }

    @Test
    public void testRedefinitionChunkedStrategyIsEnabled() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.REDEFINITION_CHUNKED.isEnabled(), is(true));
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
    public void testRetransformationChunkedStrategyIsRetransforming() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        assertThat(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION_CHUNKED.isRetransforming(instrumentation), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRetransformationChunkedStrategyNotSupportedThrowsException() throws Exception {
        AgentBuilder.RedefinitionStrategy.RETRANSFORMATION_CHUNKED.isRetransforming(mock(Instrumentation.class));
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
    public void testRedefinitionChunkedStrategyIsNotRetransforming() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        assertThat(AgentBuilder.RedefinitionStrategy.REDEFINITION_CHUNKED.isRetransforming(instrumentation), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRedefinitionChunkedStrategyNotSupportedThrowsException() throws Exception {
        AgentBuilder.RedefinitionStrategy.REDEFINITION_CHUNKED.isRetransforming(mock(Instrumentation.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.ForRedefinition.Cumulative.class).applyBasic();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.ForRedefinition.Chunked.class).applyBasic();
        final Iterator<Class<?>> iterator = Arrays.<Class<?>>asList(Object.class, String.class).iterator();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.ForRedefinition.Entry.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return iterator.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.ForRetransformation.Cumulative.class).applyBasic();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.ForRetransformation.Chunked.class).applyBasic();
    }
}
