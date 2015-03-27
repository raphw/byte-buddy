package net.bytebuddy.instrumentation.method.bytecode.stack.assign;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class AssignerEqualTypesOnlyObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Assigner.EqualTypesOnly.class).apply();
    }
}
