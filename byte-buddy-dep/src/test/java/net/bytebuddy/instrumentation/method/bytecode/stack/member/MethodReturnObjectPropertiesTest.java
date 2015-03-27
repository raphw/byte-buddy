package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class MethodReturnObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodReturn.class).apply();
    }
}
