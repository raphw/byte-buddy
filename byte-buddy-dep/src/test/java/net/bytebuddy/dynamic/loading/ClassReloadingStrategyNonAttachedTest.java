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
        HashCodeEqualsTester.of(ClassReloadingStrategy.class).refine(new HashCodeEqualsTester.Refinement<Instrumentation>() {
            @Override
            public void apply(Instrumentation mock) {
                when(mock.isRedefineClassesSupported()).thenReturn(true);
            }
        }).apply();
    }
}
