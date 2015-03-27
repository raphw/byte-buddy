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

public class AgentBuilderBinaryLocatorDefaultTest {

    private static final String FOO = "foo";

    private static final byte[] QUX = new byte[]{1, 2, 3};

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassLoader classLoader;

    @Test
    public void testTypePool() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.INSTANCE.initialize(FOO, QUX, classLoader).getTypePool(),
                notNullValue(TypePool.class));
    }

    @Test
    public void testClassFileLocator() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.INSTANCE.initialize(FOO, QUX, classLoader).getClassFileLocator(),
                notNullValue(ClassFileLocator.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.BinaryLocator.Default.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.BinaryLocator.Default.Initialized.class).apply();
    }
}
