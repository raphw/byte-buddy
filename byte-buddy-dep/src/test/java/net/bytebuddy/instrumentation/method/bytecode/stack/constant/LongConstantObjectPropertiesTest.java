package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class LongConstantObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(LongConstant.class).apply();
        ObjectPropertyAssertion.of(LongConstant.ConstantPool.class).apply();
    }
}
