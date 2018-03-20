package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class MultiplicationObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Multiplication.class).apply();
    }
}
