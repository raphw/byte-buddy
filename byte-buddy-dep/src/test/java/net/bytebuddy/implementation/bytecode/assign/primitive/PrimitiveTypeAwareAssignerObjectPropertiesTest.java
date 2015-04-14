package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class PrimitiveTypeAwareAssignerObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(PrimitiveTypeAwareAssigner.class).apply();
    }
}
