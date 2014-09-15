package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.HashCodeEqualsTester;
import org.junit.Test;

public class FieldAttributeAppenderFactoryCompoundTest {

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(FieldAttributeAppender.Factory.Compound.class).apply();
    }
}
