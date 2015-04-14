package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class StackSizeObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(StackSize.class).apply();
    }
}
