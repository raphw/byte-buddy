package net.bytebuddy.instrumentation.method.bytecode.stack;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class RemovalObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Removal.class).apply();
    }
}
