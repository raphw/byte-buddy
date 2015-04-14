package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class DuplicationObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Duplication.class).apply();
    }
}
