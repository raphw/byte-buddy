package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AgentBuilderDefaultInitializationStrategyTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private DynamicType.Builder<?> builder;

    @Test
    @SuppressWarnings("unchecked")
    public void testNoOp() throws Exception {
        assertThat(AgentBuilder.Default.InitializationStrategy.NoOp.INSTANCE.apply(builder), is((DynamicType.Builder) builder));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.Default.InitializationStrategy.NoOp.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.InitializationStrategy.SelfInjection.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.InitializationStrategy.SelfInjection.Nexus.Accessor.class).apply();
    }
}