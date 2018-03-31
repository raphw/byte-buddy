package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteArrayClassLoaderSynchronizationStrategyTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassLoader classLoader;

    @Test
    public void testInitialize() throws Exception {
        assertThat(ByteArrayClassLoader.SynchronizationStrategy.ForLegacyVm.INSTANCE.initialize(),
                is((ByteArrayClassLoader.SynchronizationStrategy) ByteArrayClassLoader.SynchronizationStrategy.ForLegacyVm.INSTANCE));
    }

    @Test
    public void testLegacyVm() throws Exception {
        assertThat(ByteArrayClassLoader.SynchronizationStrategy.ForLegacyVm.INSTANCE.getClassLoadingLock(classLoader, FOO), is((Object) classLoader));
    }
}
