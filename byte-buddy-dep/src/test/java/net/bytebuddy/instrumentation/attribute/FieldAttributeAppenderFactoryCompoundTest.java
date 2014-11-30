package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class FieldAttributeAppenderFactoryCompoundTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldAttributeAppender.Factory.Compound.class).apply();
    }
}
