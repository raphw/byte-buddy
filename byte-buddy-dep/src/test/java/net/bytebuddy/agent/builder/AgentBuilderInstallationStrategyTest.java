package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderInstallationStrategyTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassFileTransformer classFileTransformer;

    @Mock
    private Throwable throwable;

    @Mock
    private Instrumentation instrumentation;

    @Test
    public void testSuppressing() throws Exception {
        assertThat(AgentBuilder.InstallationStrategy.Default.SUPPRESSING.onError(instrumentation, classFileTransformer, throwable), is(classFileTransformer));
        verifyZeroInteractions(instrumentation);
    }

    @Test(expected = IllegalStateException.class)
    public void testEscalating() throws Exception {
        try {
            AgentBuilder.InstallationStrategy.Default.ESCALATING.onError(instrumentation, classFileTransformer, throwable);
        } finally {
            verify(instrumentation).removeTransformer(classFileTransformer);
            verifyNoMoreInteractions(instrumentation);
        }
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.InstallationStrategy.Default.class).apply();
    }
}
