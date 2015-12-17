package net.bytebuddy.agent.builder;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.instrument.Instrumentation;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentBuilderDefaultNativeMethodStrategyTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getInternalName()).thenReturn(BAR);
    }

    @Test(expected = IllegalStateException.class)
    public void testDisabledStrategyThrowsExceptionForPrefix() throws Exception {
        AgentBuilder.Default.NativeMethodStrategy.Disabled.INSTANCE.getPrefix();
    }

    @Test
    public void testDisabledStrategyIsDisabled() throws Exception {
        assertThat(AgentBuilder.Default.NativeMethodStrategy.Disabled.INSTANCE.isEnabled(mock(Instrumentation.class)), is(false));
    }

    @Test
    public void testDisabledStrategySuffixesNames() throws Exception {
        assertThat(AgentBuilder.Default.NativeMethodStrategy.Disabled.INSTANCE.resolve().transform(methodDescription), startsWith(BAR));
        assertThat(AgentBuilder.Default.NativeMethodStrategy.Disabled.INSTANCE.resolve().transform(methodDescription), not(BAR));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnabledStrategyMustNotBeEmptyString() throws Exception {
        AgentBuilder.Default.NativeMethodStrategy.ForPrefix.of("");
    }

    @Test
    public void testEnabledStrategyReturnsPrefix() throws Exception {
        assertThat(new AgentBuilder.Default.NativeMethodStrategy.ForPrefix(FOO).getPrefix(), is(FOO));
    }

    @Test
    public void testEnabledStrategyIsEnabled() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        when(instrumentation.isNativeMethodPrefixSupported()).thenReturn(true);
        assertThat(new AgentBuilder.Default.NativeMethodStrategy.ForPrefix(FOO).isEnabled(instrumentation), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnabledStrategyThrowsExceptionIfNotSupported() throws Exception {
        new AgentBuilder.Default.NativeMethodStrategy.ForPrefix(FOO).isEnabled(mock(Instrumentation.class));
    }

    @Test
    public void testEnabledStrategySuffixesNames() throws Exception {
        assertThat(new AgentBuilder.Default.NativeMethodStrategy.ForPrefix(FOO).resolve().transform(methodDescription), is(FOO + BAR));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.Default.NativeMethodStrategy.Disabled.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.NativeMethodStrategy.ForPrefix.class).apply();
    }
}