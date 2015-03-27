package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class PrimitiveBoxingDelegateObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(PrimitiveBoxingDelegate.class).apply();
    }
}
