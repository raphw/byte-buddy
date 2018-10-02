package net.bytebuddy.build;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.MatcherAssert.assertThat;

public class PluginEnginePoolStrategyTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassFileLocator classFileLocator;

    @Test
    public void testWithLazyResolutionFast() {
        assertThat(Plugin.Engine.PoolStrategy.Default.FAST.typePool(classFileLocator),
                hasPrototype((TypePool) new TypePool.Default.WithLazyResolution(new TypePool.CacheProvider.Simple(),
                        classFileLocator,
                        TypePool.Default.ReaderMode.FAST,
                        TypePool.ClassLoading.ofPlatformLoader())));
    }

    @Test
    public void testWithLazyResolutionExtended() {
        assertThat(Plugin.Engine.PoolStrategy.Default.EXTENDED.typePool(classFileLocator),
                hasPrototype((TypePool) new TypePool.Default.WithLazyResolution(new TypePool.CacheProvider.Simple(),
                        classFileLocator,
                        TypePool.Default.ReaderMode.EXTENDED,
                        TypePool.ClassLoading.ofPlatformLoader())));
    }

    @Test
    public void testWithEagerResolutionFast() {
        assertThat(Plugin.Engine.PoolStrategy.Eager.FAST.typePool(classFileLocator),
                hasPrototype((TypePool) new TypePool.Default(new TypePool.CacheProvider.Simple(),
                        classFileLocator,
                        TypePool.Default.ReaderMode.FAST,
                        TypePool.ClassLoading.ofPlatformLoader())));
    }

    @Test
    public void testWithEagerResolutionExtended() {
        assertThat(Plugin.Engine.PoolStrategy.Eager.EXTENDED.typePool(classFileLocator),
                hasPrototype((TypePool) new TypePool.Default(new TypePool.CacheProvider.Simple(),
                        classFileLocator,
                        TypePool.Default.ReaderMode.EXTENDED,
                        TypePool.ClassLoading.ofPlatformLoader())));
    }
}
