package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.utility.HashCodeEqualsTester;
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
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(ClassFileLocator.AgentBased.class).refine(new HashCodeEqualsTester.Refinement() {
            @Override
            public void apply(Object mock) {
                if (Instrumentation.class.isAssignableFrom(mock.getClass())) {
                    when(((Instrumentation) mock).isRetransformClassesSupported()).thenReturn(true);
                }
            }
        }).apply();
    }
}
