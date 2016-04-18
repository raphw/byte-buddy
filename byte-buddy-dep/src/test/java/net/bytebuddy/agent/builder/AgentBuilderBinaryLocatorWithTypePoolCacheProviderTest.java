package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;


public class AgentBuilderBinaryLocatorWithTypePoolCacheProviderTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassFileLocator classFileLocator;

    @Mock
    private ClassLoader first, second;

    @Test
    public void testSimpleImplementation() throws Exception {
        Map<ClassLoader, TypePool.CacheProvider> cacheProviders = new HashMap<ClassLoader, TypePool.CacheProvider>();
        AgentBuilder.BinaryLocator binaryLocator = new MapBinaryLocator(TypePool.Default.ReaderMode.FAST, cacheProviders);
        assertThat(binaryLocator.typePool(classFileLocator, first), is(binaryLocator.typePool(classFileLocator, first)));
        assertThat(binaryLocator.typePool(classFileLocator, first), not(binaryLocator.typePool(classFileLocator, second)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(VoidLocator.class).apply();
    }

    private static class MapBinaryLocator extends AgentBuilder.BinaryLocator.WithTypePoolCache {

        private final Map<ClassLoader, TypePool.CacheProvider> cacheProviders;

        protected MapBinaryLocator(TypePool.Default.ReaderMode readerMode, Map<ClassLoader, TypePool.CacheProvider> cacheProviders) {
            super(readerMode);
            this.cacheProviders = cacheProviders;
        }

        @Override
        protected TypePool.CacheProvider locate(ClassLoader classLoader) {
            TypePool.CacheProvider cacheProvider = cacheProviders.get(classLoader);
            if (cacheProvider == null) {
                cacheProvider = new TypePool.CacheProvider.Simple();
                cacheProviders.put(classLoader, cacheProvider);
            }
            return cacheProvider;
        }
    }

    private static class VoidLocator extends AgentBuilder.BinaryLocator.WithTypePoolCache {

        public VoidLocator(TypePool.Default.ReaderMode readerMode) {
            super(readerMode);
        }

        @Override
        protected TypePool.CacheProvider locate(ClassLoader classLoader) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "AgentBuilderBinaryLocatorWithTypePoolCacheProviderTest.VoidLocator{}";
        }
    }
}