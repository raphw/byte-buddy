package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class MethodCallProxyObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodCallProxy.class).apply();
        ObjectPropertyAssertion.of(MethodCallProxy.AssignableSignatureCall.class).apply();
        ObjectPropertyAssertion.of(MethodCallProxy.ConstructorCall.Appender.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(MethodCallProxy.MethodCall.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(MethodCallProxy.MethodCall.Appender.class).skipSynthetic().apply();
    }
}
