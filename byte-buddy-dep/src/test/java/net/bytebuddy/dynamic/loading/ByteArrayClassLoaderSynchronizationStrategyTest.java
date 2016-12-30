package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

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

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteArrayClassLoader.SynchronizationStrategy.CreationAction.class).apply();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.SynchronizationStrategy.ForLegacyVm.class).apply();
        final Iterator<Method> iterator = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.SynchronizationStrategy.ForJava7CapableVm.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return iterator.next();
            }
        }).apply();
    }
}
