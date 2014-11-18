package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class MethodRegistryLatentMethodMatcherSimpleTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodRegistry.LatentMethodMatcher.Simple.class).apply();
    }
}
