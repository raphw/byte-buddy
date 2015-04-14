package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class RemovalObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Removal.class).apply();
    }
}
