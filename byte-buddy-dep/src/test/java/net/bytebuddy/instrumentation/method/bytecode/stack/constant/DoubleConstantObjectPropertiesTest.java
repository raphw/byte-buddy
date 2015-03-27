package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class DoubleConstantObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(DoubleConstant.class).apply();
        ObjectPropertyAssertion.of(DoubleConstant.ConstantPool.class).apply();
    }
}
