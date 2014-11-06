package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class MethodAttributeAppenderFactoryCompoundTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodAttributeAppender.Factory.Compound.class).apply();
    }
}
