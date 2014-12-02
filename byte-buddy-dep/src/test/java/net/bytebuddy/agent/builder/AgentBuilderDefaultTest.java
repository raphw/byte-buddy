package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

public class AgentBuilderDefaultTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.Default.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Matched.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Entry.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.ExecutingTransformer.class)
                .apply(new AgentBuilder.Default().new ExecutingTransformer());
        ObjectPropertyAssertion.of(AgentBuilder.Default.InitializationStrategy.SelfInjection.class).apply();
        final Iterator<Class<?>> iterator = Arrays.<Class<?>>asList(Object.class, AgentBuilderDefaultTest.class).iterator();
        ObjectPropertyAssertion.of(AgentBuilder.Default.InitializationStrategy.SelfInjection.Nexus.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return iterator.next();
            }
        }).apply();
    }
}
