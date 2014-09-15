package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.HashCodeEqualsTester;
import org.junit.Test;

public class MethodAttributeAppenderFactoryCompoundTest {

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(MethodAttributeAppender.Factory.Compound.class).apply();
    }
}
