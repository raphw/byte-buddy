package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class DefaultValueObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(DefaultValue.class).apply();
    }
}
