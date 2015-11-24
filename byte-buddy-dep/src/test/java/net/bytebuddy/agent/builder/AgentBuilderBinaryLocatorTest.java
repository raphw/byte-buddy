package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AgentBuilderBinaryLocatorTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassLoader classLoader;

    @Mock
    private ClassFileLocator classFileLocator;

    @Test
    public void testFastClassFileLocator() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.FAST.classFileLocator(classLoader),
                is(ClassFileLocator.ForClassLoader.of(classLoader)));
    }

    @Test
    public void testExtendedClassFileLocator() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.EXTENDED.classFileLocator(classLoader),
                is(ClassFileLocator.ForClassLoader.of(classLoader)));
    }

    @Test
    public void testLoadingClassFileLocator() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.ClassLoading.INSTANCE.classFileLocator(classLoader),
                is((ClassFileLocator) ClassFileLocator.NoOp.INSTANCE));
    }

    @Test
    public void testFastTypePool() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.FAST.typePool(classFileLocator, classLoader), notNullValue(TypePool.class));
    }

    @Test
    public void testExtendedTypePool() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.EXTENDED.typePool(classFileLocator, classLoader), notNullValue(TypePool.class));
    }

    @Test
    public void testLoadingTypePool() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.ClassLoading.INSTANCE.typePool(classFileLocator, classLoader), notNullValue(TypePool.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.BinaryLocator.Default.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.BinaryLocator.ClassLoading.class).apply();
    }
}
