package net.bytebuddy.agent.builder;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AgentBuilderRedefinitionStrategyDiscoveryStrategyExplicitTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Instrumentation instrumentation;

    @Test
    public void testExplicit() throws Exception {
        Iterator<Iterable<Class<?>>> types = new AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Explicit(String.class, Integer.class)
                .resolve(instrumentation).iterator();
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), CoreMatchers.<Iterable<Class<?>>>equalTo(new HashSet<Class<?>>(Arrays.<Class<?>>asList(String.class, Integer.class))));
        assertThat(types.hasNext(), is(false));
    }
}
