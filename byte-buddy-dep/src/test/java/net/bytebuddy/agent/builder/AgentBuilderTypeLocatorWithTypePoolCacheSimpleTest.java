package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;


public class AgentBuilderTypeLocatorWithTypePoolCacheSimpleTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassFileLocator classFileLocator;

    @Mock
    private ClassLoader first, second;

    @Mock
    private TypePool.CacheProvider firstCache, secondCache;

    @Test
    public void testSimpleImplementation() throws Exception {
        ConcurrentMap<ClassLoader, TypePool.CacheProvider> cacheProviders = new ConcurrentHashMap<ClassLoader, TypePool.CacheProvider>();
        cacheProviders.put(first, firstCache);
        cacheProviders.put(second, secondCache);
        AgentBuilder.PoolStrategy poolStrategy = new AgentBuilder.PoolStrategy.WithTypePoolCache.Simple(TypePool.Default.ReaderMode.FAST, cacheProviders);
        assertThat(poolStrategy.typePool(classFileLocator, first), hasPrototype(poolStrategy.typePool(classFileLocator, first)));
        assertThat(poolStrategy.typePool(classFileLocator, first), not(hasPrototype(poolStrategy.typePool(classFileLocator, second))));
    }

    @Test
    public void testSimpleImplementationBootstrap() throws Exception {
        ConcurrentMap<ClassLoader, TypePool.CacheProvider> cacheProviders = new ConcurrentHashMap<ClassLoader, TypePool.CacheProvider>();
        cacheProviders.put(ClassLoader.getSystemClassLoader(), firstCache);
        cacheProviders.put(second, secondCache);
        AgentBuilder.PoolStrategy poolStrategy = new AgentBuilder.PoolStrategy.WithTypePoolCache.Simple(TypePool.Default.ReaderMode.FAST, cacheProviders);
        assertThat(poolStrategy.typePool(classFileLocator, null), hasPrototype(poolStrategy.typePool(classFileLocator, null)));
        assertThat(poolStrategy.typePool(classFileLocator, null), not(hasPrototype(poolStrategy.typePool(classFileLocator, second))));
    }
}
