package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

public class PrimitiveUnboxingDelegateOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSourceTypeThrowsException() throws Exception {
        PrimitiveUnboxingDelegate.forReferenceType(new TypeDescription.Generic.OfNonGenericType.ForLoadedType(int.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVoidIllegal() throws Exception {
        PrimitiveUnboxingDelegate.forPrimitive(TypeDescription.Generic.VOID);
    }
}
