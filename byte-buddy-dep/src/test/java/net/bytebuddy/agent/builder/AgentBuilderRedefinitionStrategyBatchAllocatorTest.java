package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AgentBuilderRedefinitionStrategyBatchAllocatorTest {

    @Test
    public void testForTotal() throws Exception {
        AgentBuilder.RedefinitionStrategy.BatchAllocator batchAllocator = AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE;
        Iterator<? extends List<Class<?>>> iterator = batchAllocator.batch(Arrays.<Class<?>>asList(Object.class, Void.class, String.class)).iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(Arrays.<Class<?>>asList(Object.class, Void.class, String.class)));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void testForFixed() throws Exception {
        AgentBuilder.RedefinitionStrategy.BatchAllocator batchAllocator = new AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize(2);
        Iterator<? extends List<Class<?>>> iterator = batchAllocator.batch(Arrays.<Class<?>>asList(Object.class, Void.class, String.class)).iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(Arrays.<Class<?>>asList(Object.class, Void.class)));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(Collections.<Class<?>>singletonList(String.class)));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void testForFixedFactory() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize.ofSize(1),
                is((AgentBuilder.RedefinitionStrategy.BatchAllocator) new AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize(1)));
        assertThat(AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize.ofSize(0),
                is((AgentBuilder.RedefinitionStrategy.BatchAllocator) AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForFixedFactoryIllegal() throws Exception {
        AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize.ofSize(-1);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize.class).apply();
    }
}
