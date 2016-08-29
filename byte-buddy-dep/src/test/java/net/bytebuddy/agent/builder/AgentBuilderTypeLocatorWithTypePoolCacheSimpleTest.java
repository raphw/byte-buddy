package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;


public class AgentBuilderTypeLocatorWithTypePoolCacheSimpleTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassFileLocator classFileLocator;

    @Mock
    private ClassLoader first, second;

    @Test
    public void testSimpleImplementation() throws Exception {
        ConcurrentMap<ClassLoader, TypePool.CacheProvider> cacheProviders = new ConcurrentHashMap<ClassLoader, TypePool.CacheProvider>();
        AgentBuilder.PoolStrategy poolStrategy = new AgentBuilder.PoolStrategy.WithTypePoolCache.Simple(TypePool.Default.ReaderMode.FAST, cacheProviders);
        assertThat(poolStrategy.typePool(classFileLocator, first), is(poolStrategy.typePool(classFileLocator, first)));
        assertThat(poolStrategy.typePool(classFileLocator, first), not(poolStrategy.typePool(classFileLocator, second)));
    }

    @Test
    public void testSimpleImplementationBootstrap() throws Exception {
        ConcurrentMap<ClassLoader, TypePool.CacheProvider> cacheProviders = new ConcurrentHashMap<ClassLoader, TypePool.CacheProvider>();
        AgentBuilder.PoolStrategy poolStrategy = new AgentBuilder.PoolStrategy.WithTypePoolCache.Simple(TypePool.Default.ReaderMode.FAST, cacheProviders);
        assertThat(poolStrategy.typePool(classFileLocator, null), is(poolStrategy.typePool(classFileLocator, null)));
        assertThat(poolStrategy.typePool(classFileLocator, null), not(poolStrategy.typePool(classFileLocator, second)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.PoolStrategy.WithTypePoolCache.Simple.class).apply();
    }
}
