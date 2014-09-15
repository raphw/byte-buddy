package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.utility.HashCodeEqualsTester;
import org.junit.Test;

public class MethodRegistryLatentMethodMatcherSimpleTest {

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(MethodRegistry.LatentMethodMatcher.Simple.class).apply();
    }
}
