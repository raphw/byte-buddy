package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.utility.HashCodeEqualsTester;
import org.junit.Test;

public class MethodCallProxyEqualsHashCodeTest {

    @Test
    public void testEqualsHashCode() throws Exception {
        HashCodeEqualsTester.of(MethodCallProxy.class).apply();
    }

    @Test
    public void testAssignableSignatureCallEqualsHashCode() throws Exception {
        HashCodeEqualsTester.of(MethodCallProxy.AssignableSignatureCall.class).apply();
    }
}
