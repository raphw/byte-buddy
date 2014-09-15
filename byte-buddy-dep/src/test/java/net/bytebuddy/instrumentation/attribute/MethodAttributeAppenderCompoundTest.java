package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.HashCodeEqualsTester;
import org.junit.Test;

public class MethodAttributeAppenderCompoundTest {

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(MethodAttributeAppender.Compound.class).apply();
    }
}
