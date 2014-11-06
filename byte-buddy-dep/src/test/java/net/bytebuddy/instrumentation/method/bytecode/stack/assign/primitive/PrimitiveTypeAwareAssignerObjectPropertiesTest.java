package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class PrimitiveTypeAwareAssignerObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(PrimitiveTypeAwareAssigner.class).apply();
    }
}
