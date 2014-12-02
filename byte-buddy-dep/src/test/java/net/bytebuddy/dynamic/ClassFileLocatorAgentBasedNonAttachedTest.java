package net.bytebuddy.dynamic;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.instrument.Instrumentation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClassFileLocatorAgentBasedNonAttachedTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNonCompatible() throws Exception {
        new ClassFileLocator.AgentBased(mock(Instrumentation.class), getClass().getClassLoader());
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassFileLocator.AgentBased.class).refine(new ObjectPropertyAssertion.Refinement<Instrumentation>() {
            @Override
            public void apply(Instrumentation mock) {
                when(mock.isRetransformClassesSupported()).thenReturn(true);
            }
        }).apply();
    }
}
