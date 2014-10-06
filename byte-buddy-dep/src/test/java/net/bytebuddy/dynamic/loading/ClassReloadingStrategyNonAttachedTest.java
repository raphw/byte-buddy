package net.bytebuddy.dynamic.loading;

import net.bytebuddy.utility.HashCodeEqualsTester;
import org.junit.Test;

import java.lang.instrument.Instrumentation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClassReloadingStrategyNonAttachedTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNonCompatible() throws Exception {
        new ClassReloadingStrategy(mock(Instrumentation.class));
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(ClassReloadingStrategy.class).refine(new HashCodeEqualsTester.Refinement() {
            @Override
            public void apply(Object mock) {
                if (Instrumentation.class.isAssignableFrom(mock.getClass())) {
                    when(((Instrumentation) mock).isRedefineClassesSupported()).thenReturn(true);
                }
            }
        }).apply();
    }
}
