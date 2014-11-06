package net.bytebuddy.instrumentation;

import net.bytebuddy.utility.HashCodeEqualsTester;
import org.junit.Test;

public class InstrumentationSimpleTest {

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(Instrumentation.Simple.class).apply();
    }
}
