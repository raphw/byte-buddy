package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

public class AgentBuilderRedefinitionStrategy {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.ForRedefinition.class).applyBasic();
        final Iterator<Class<?>> iterator = Arrays.<Class<?>>asList(Object.class, String.class).iterator();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.ForRedefinition.Entry.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return iterator.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.Collector.ForRetransformation.class).applyBasic();
    }
}
