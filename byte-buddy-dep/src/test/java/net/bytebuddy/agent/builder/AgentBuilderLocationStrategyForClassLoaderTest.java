package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AgentBuilderLocationStrategyForClassLoaderTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassLoader classLoader;

    @Mock
    private JavaModule module;

    @Test
    public void testStrongLocationStrategy() throws Exception {
        assertThat(AgentBuilder.LocationStrategy.ForClassLoader.STRONG.classFileLocator(classLoader, module), is(ClassFileLocator.ForClassLoader.of(classLoader)));
    }

    @Test
    public void testWeakLocationStrategy() throws Exception {
        assertThat(AgentBuilder.LocationStrategy.ForClassLoader.WEAK.classFileLocator(classLoader, module), is(ClassFileLocator.ForClassLoader.WeaklyReferenced.of(classLoader)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.LocationStrategy.ForClassLoader.class).apply();
    }
}
