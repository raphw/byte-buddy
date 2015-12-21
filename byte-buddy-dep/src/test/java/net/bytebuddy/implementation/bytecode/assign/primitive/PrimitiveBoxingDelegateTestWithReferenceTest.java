package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

public class PrimitiveBoxingDelegateTestWithReferenceTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsException() throws Exception {
        PrimitiveBoxingDelegate.forPrimitive(TypeDescription.OBJECT);
    }
}
