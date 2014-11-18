package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class MethodAttributeAppenderCompoundTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodAttributeAppender.Compound.class).apply();
    }
}
