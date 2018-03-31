package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AgentBuilderPoolStrategyTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassLoader classLoader;

    @Mock
    private ClassFileLocator classFileLocator;

    @Test
    public void testFastTypePool() throws Exception {
        assertThat(AgentBuilder.PoolStrategy.Default.FAST.typePool(classFileLocator, classLoader), notNullValue(TypePool.class));
    }

    @Test
    public void testExtendedTypePool() throws Exception {
        assertThat(AgentBuilder.PoolStrategy.Default.EXTENDED.typePool(classFileLocator, classLoader), notNullValue(TypePool.class));
    }

    @Test
    public void testFastEagerTypePool() throws Exception {
        assertThat(AgentBuilder.PoolStrategy.Eager.FAST.typePool(classFileLocator, classLoader), notNullValue(TypePool.class));
    }

    @Test
    public void testExtendedEagerTypePool() throws Exception {
        assertThat(AgentBuilder.PoolStrategy.Eager.EXTENDED.typePool(classFileLocator, classLoader), notNullValue(TypePool.class));
    }

    @Test
    public void testFastLoadingTypePool() throws Exception {
        assertThat(AgentBuilder.PoolStrategy.ClassLoading.FAST.typePool(classFileLocator, classLoader), notNullValue(TypePool.class));
    }

    @Test
    public void testExtendedLoadingTypePool() throws Exception {
        assertThat(AgentBuilder.PoolStrategy.ClassLoading.EXTENDED.typePool(classFileLocator, classLoader), notNullValue(TypePool.class));
    }
}
