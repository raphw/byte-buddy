package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.HashCodeEqualsTester;
import org.junit.Test;

public class TypeAttributeAppenderCompoundTest {

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(TypeAttributeAppender.Compound.class).apply();
    }
}
