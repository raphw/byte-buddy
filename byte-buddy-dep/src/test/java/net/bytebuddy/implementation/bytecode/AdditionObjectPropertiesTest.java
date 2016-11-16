package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class AdditionObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Addition.class).apply();
    }
}
