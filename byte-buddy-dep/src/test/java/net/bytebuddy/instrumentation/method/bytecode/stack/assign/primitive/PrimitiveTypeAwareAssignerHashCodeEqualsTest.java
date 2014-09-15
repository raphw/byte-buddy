package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.utility.HashCodeEqualsTester;
import org.junit.Test;

public class PrimitiveTypeAwareAssignerHashCodeEqualsTest {

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(PrimitiveTypeAwareAssigner.class).apply();
    }
}
