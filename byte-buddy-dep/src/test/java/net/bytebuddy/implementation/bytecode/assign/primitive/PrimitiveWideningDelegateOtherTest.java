package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

public class PrimitiveWideningDelegateOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSourceTypeThrowsException() throws Exception {
        PrimitiveWideningDelegate.forPrimitive(TypeDescription.ForLoadedType.of(Object.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTargetTypeThrowsException() throws Exception {
        PrimitiveWideningDelegate.forPrimitive(TypeDescription.ForLoadedType.of(int.class)).widenTo(TypeDescription.ForLoadedType.of(Object.class));
    }
}
