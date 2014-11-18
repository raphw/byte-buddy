package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class FieldAttributeAppenderCompoundTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldAttributeAppender.Compound.class).apply();
    }
}
