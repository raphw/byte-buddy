package net.bytebuddy.instrumentation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class InstrumentationSimpleTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Instrumentation.Simple.class).apply();
    }
}
