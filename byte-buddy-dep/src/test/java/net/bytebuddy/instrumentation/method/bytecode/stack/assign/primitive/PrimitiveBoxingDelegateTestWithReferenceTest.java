package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;

public class PrimitiveBoxingDelegateTestWithReferenceTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsException() throws Exception {
        PrimitiveBoxingDelegate.forPrimitive(new TypeDescription.ForLoadedType(Object.class));
    }
}
