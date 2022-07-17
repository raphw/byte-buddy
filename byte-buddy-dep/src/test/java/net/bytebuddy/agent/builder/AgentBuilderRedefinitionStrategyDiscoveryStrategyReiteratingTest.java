package net.bytebuddy.agent.builder;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.lang.instrument.Instrumentation;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class AgentBuilderRedefinitionStrategyDiscoveryStrategyReiteratingTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE},
                {AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.WithSortOrderAssumption.INSTANCE}
        });
    }

    private final AgentBuilder.RedefinitionStrategy.DiscoveryStrategy discoveryStrategy;

    public AgentBuilderRedefinitionStrategyDiscoveryStrategyReiteratingTest(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy discoveryStrategy) {
        this.discoveryStrategy = discoveryStrategy;
    }

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Instrumentation instrumentation;

    @Test
    public void testSinglePass() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{String.class, Integer.class});
        Iterator<Iterable<Class<?>>> types = discoveryStrategy.resolve(instrumentation).iterator();
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), CoreMatchers.<Iterable<Class<?>>>equalTo(Arrays.<Class<?>>asList(String.class, Integer.class)));
        assertThat(types.hasNext(), is(false));
    }

    @Test
    public void testReiteration() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{String.class, Integer.class},
                new Class<?>[]{String.class, Integer.class, Void.class});
        Iterator<Iterable<Class<?>>> types = discoveryStrategy.resolve(instrumentation).iterator();
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), CoreMatchers.<Iterable<Class<?>>>equalTo(Arrays.<Class<?>>asList(String.class, Integer.class)));
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), CoreMatchers.<Iterable<Class<?>>>equalTo(Collections.<Class<?>>singletonList(Void.class)));
        assertThat(types.hasNext(), is(false));
    }

    @Test(expected = NoSuchElementException.class)
    public void testReiterationNoMoreElement() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[0]);
        discoveryStrategy.resolve(instrumentation).iterator().next();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReiterationNoRemoval() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{Void.class});
        discoveryStrategy.resolve(instrumentation).iterator().remove();
    }
}
