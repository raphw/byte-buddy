package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.HashCodeEqualsTester;
import org.junit.Test;

public class FieldAttributeAppenderCompoundTest {

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(FieldAttributeAppender.Compound.class).apply();
    }
}
