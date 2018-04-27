package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

public class PrimitiveUnboxingDelegateOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSourceTypeThrowsException() throws Exception {
        PrimitiveUnboxingDelegate.forReferenceType(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(int.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVoidIllegal() throws Exception {
        PrimitiveUnboxingDelegate.forPrimitive(TypeDescription.Generic.VOID);
    }
}
