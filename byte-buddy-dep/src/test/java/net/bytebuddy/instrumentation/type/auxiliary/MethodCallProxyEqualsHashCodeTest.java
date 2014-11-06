package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class MethodCallProxyEqualsHashCodeTest {

    @Test
    public void testEqualsHashCode() throws Exception {
        ObjectPropertyAssertion.of(MethodCallProxy.class).apply();
    }

    @Test
    public void testAssignableSignatureCallEqualsHashCode() throws Exception {
        ObjectPropertyAssertion.of(MethodCallProxy.AssignableSignatureCall.class).apply();
    }
}
