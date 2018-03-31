package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

public class PrimitiveWideningDelegateOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSourceTypeThrowsException() throws Exception {
        PrimitiveWideningDelegate.forPrimitive(TypeDescription.OBJECT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTargetTypeThrowsException() throws Exception {
        PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(int.class)).widenTo(TypeDescription.OBJECT);
    }
}
