package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.MatcherAssert.assertThat;

public class AgentBuilderLocationStrategyForClassLoaderTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ClassLoader classLoader;

    @Mock
    private JavaModule module;

    @Mock
    private AgentBuilder.LocationStrategy fallback;

    @Mock
    private ClassFileLocator classFileLocator;

    @Test
    public void testStrongLocationStrategy() throws Exception {
        assertThat(AgentBuilder.LocationStrategy.ForClassLoader.STRONG.classFileLocator(classLoader, module),
                hasPrototype(ClassFileLocator.ForClassLoader.of(classLoader)));
    }

    @Test
    public void testWeakLocationStrategy() throws Exception {
        assertThat(AgentBuilder.LocationStrategy.ForClassLoader.WEAK.classFileLocator(classLoader, module),
                hasPrototype(ClassFileLocator.ForClassLoader.WeaklyReferenced.of(classLoader)));
    }

    @Test
    public void testFallback() throws Exception {
        assertThat(AgentBuilder.LocationStrategy.ForClassLoader.STRONG.withFallbackTo(fallback),
                hasPrototype((AgentBuilder.LocationStrategy) new AgentBuilder.LocationStrategy.Compound(AgentBuilder.LocationStrategy.ForClassLoader.STRONG, fallback)));
        assertThat(AgentBuilder.LocationStrategy.ForClassLoader.WEAK.withFallbackTo(fallback),
                hasPrototype((AgentBuilder.LocationStrategy) new AgentBuilder.LocationStrategy.Compound(AgentBuilder.LocationStrategy.ForClassLoader.WEAK, fallback)));
    }

    @Test
    public void testFallbackLocator() throws Exception {
        assertThat(AgentBuilder.LocationStrategy.ForClassLoader.STRONG.withFallbackTo(classFileLocator),
                hasPrototype((AgentBuilder.LocationStrategy) new AgentBuilder.LocationStrategy.Compound(AgentBuilder.LocationStrategy.ForClassLoader.STRONG, new AgentBuilder.LocationStrategy.Simple(classFileLocator))));
        assertThat(AgentBuilder.LocationStrategy.ForClassLoader.WEAK.withFallbackTo(classFileLocator),
                hasPrototype((AgentBuilder.LocationStrategy) new AgentBuilder.LocationStrategy.Compound(AgentBuilder.LocationStrategy.ForClassLoader.WEAK, new AgentBuilder.LocationStrategy.Simple(classFileLocator))));
    }
}
