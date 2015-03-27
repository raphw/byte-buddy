package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class FieldAccessObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldAccess.class).apply();
        ObjectPropertyAssertion.of(FieldAccess.AccessDispatcher.class).apply();
        ObjectPropertyAssertion.of(FieldAccess.AccessDispatcher.FieldGetInstruction.class).apply();
        ObjectPropertyAssertion.of(FieldAccess.AccessDispatcher.FieldPutInstruction.class).apply();
    }
}
