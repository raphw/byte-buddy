package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.MockitoRule;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.instrument.Instrumentation;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class AgentBuilderRedefinitionStrategyDiscoveryStrategyTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation instrumentation;

    @Test
    public void testSinglePass() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{String.class, Integer.class});
        Iterator<Iterable<Class<?>>> types = AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE.resolve(instrumentation).iterator();
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), CoreMatchers.<Iterable<Class<?>>>equalTo(Arrays.<Class<?>>asList(String.class, Integer.class)));
        assertThat(types.hasNext(), is(false));
    }

    @Test
    public void testReiteration() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{String.class, Integer.class},
                new Class<?>[]{String.class, Integer.class, Void.class});
        Iterator<Iterable<Class<?>>> types = AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE.resolve(instrumentation).iterator();
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), CoreMatchers.<Iterable<Class<?>>>equalTo(Arrays.<Class<?>>asList(String.class, Integer.class)));
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), CoreMatchers.<Iterable<Class<?>>>equalTo(Collections.<Class<?>>singletonList(Void.class)));
        assertThat(types.hasNext(), is(false));
    }

    @Test(expected = NoSuchElementException.class)
    public void testReiterationNoMoreElement() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[0]);
        AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE.resolve(instrumentation).iterator().next();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReiterationNoRemoval() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{Void.class});
        AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE.resolve(instrumentation).iterator().remove();
    }

    @Test
    public void testExplicit() throws Exception {
        Iterator<Iterable<Class<?>>> types = new AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Explicit(String.class, Integer.class)
                .resolve(instrumentation).iterator();
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), CoreMatchers.<Iterable<Class<?>>>equalTo(new HashSet<Class<?>>(Arrays.<Class<?>>asList(String.class, Integer.class))));
        assertThat(types.hasNext(), is(false));
    }
}
