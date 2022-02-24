package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.MockitoRule;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import java.lang.instrument.Instrumentation;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class AgentBuilderRedefinitionStrategyDiscoveryStrategyExplicitTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

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
