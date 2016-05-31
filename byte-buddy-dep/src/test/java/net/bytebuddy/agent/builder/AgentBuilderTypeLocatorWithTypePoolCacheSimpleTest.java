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
        AgentBuilder.TypeLocator typeLocator = new AgentBuilder.TypeLocator.WithTypePoolCache.Simple(TypePool.Default.ReaderMode.FAST, cacheProviders);
        assertThat(typeLocator.typePool(classFileLocator, first), is(typeLocator.typePool(classFileLocator, first)));
        assertThat(typeLocator.typePool(classFileLocator, first), not(typeLocator.typePool(classFileLocator, second)));
    }

    @Test
    public void testSimpleImplementationBootstrap() throws Exception {
        ConcurrentMap<ClassLoader, TypePool.CacheProvider> cacheProviders = new ConcurrentHashMap<ClassLoader, TypePool.CacheProvider>();
        AgentBuilder.TypeLocator typeLocator = new AgentBuilder.TypeLocator.WithTypePoolCache.Simple(TypePool.Default.ReaderMode.FAST, cacheProviders);
        assertThat(typeLocator.typePool(classFileLocator, null), is(typeLocator.typePool(classFileLocator, null)));
        assertThat(typeLocator.typePool(classFileLocator, null), not(typeLocator.typePool(classFileLocator, second)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.TypeLocator.WithTypePoolCache.Simple.class).apply();
    }
}