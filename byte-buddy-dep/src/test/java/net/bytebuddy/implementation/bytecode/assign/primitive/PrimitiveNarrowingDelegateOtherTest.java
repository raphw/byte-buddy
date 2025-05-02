package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

public class PrimitiveNarrowingDelegateOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSourceTypeThrowsException() throws Exception {
        PrimitiveNarrowingDelegate.forPrimitive(TypeDescription.ForLoadedType.of(Object.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTargetTypeThrowsException() throws Exception {
        PrimitiveNarrowingDelegate.forPrimitive(TypeDescription.ForLoadedType.of(int.class)).narrowTo(TypeDescription.ForLoadedType.of(Object.class));
    }
}
