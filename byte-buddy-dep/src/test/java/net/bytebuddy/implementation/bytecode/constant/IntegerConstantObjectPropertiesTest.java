package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class IntegerConstantObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(IntegerConstant.class).apply();
        ObjectPropertyAssertion.of(IntegerConstant.ConstantPool.class).apply();
        ObjectPropertyAssertion.of(IntegerConstant.SingleBytePush.class).apply();
        ObjectPropertyAssertion.of(IntegerConstant.TwoBytePush.class).apply();
    }
}
