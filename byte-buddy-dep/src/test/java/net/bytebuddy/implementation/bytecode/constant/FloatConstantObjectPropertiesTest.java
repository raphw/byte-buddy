package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class FloatConstantObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FloatConstant.class).apply();
        ObjectPropertyAssertion.of(FloatConstant.ConstantPool.class).apply();
    }
}
