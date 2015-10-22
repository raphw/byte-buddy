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

public class AgentBuilderBinaryLocatorTest {

    private static final String FOO = "foo";

    private static final byte[] QUX = new byte[]{1, 2, 3};

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassLoader classLoader;

    @Test
    public void testTypePoolExtended() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.EXTENDED.initialize(classLoader).getTypePool(),
                notNullValue(TypePool.class));
    }

    @Test
    public void testClassFileLocatorExtended() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.EXTENDED.initialize(classLoader).getClassFileLocator(),
                notNullValue(ClassFileLocator.class));
    }

    @Test
    public void testTypePoolExtendedWithExplicitType() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.EXTENDED.initialize(classLoader, FOO, QUX).getTypePool(),
                notNullValue(TypePool.class));
    }

    @Test
    public void testClassFileLocatorExtendedWithExplicitType() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.EXTENDED.initialize(classLoader, FOO, QUX).getClassFileLocator(),
                notNullValue(ClassFileLocator.class));
    }

    @Test
    public void testTypePoolFast() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.FAST.initialize(classLoader).getTypePool(),
                notNullValue(TypePool.class));
    }

    @Test
    public void testClassFileLocatorFast() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.FAST.initialize(classLoader).getClassFileLocator(),
                notNullValue(ClassFileLocator.class));
    }

    @Test
    public void testTypePoolFastWithExplicitType() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.FAST.initialize(classLoader, FOO, QUX).getTypePool(),
                notNullValue(TypePool.class));
    }

    @Test
    public void testClassFileLocatorFastWithExplicitType() throws Exception {
        assertThat(AgentBuilder.BinaryLocator.Default.FAST.initialize(classLoader, FOO, QUX).getClassFileLocator(),
                notNullValue(ClassFileLocator.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.BinaryLocator.Default.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.BinaryLocator.Initialized.Simple.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.BinaryLocator.Initialized.Extended.class).apply();
    }
}
