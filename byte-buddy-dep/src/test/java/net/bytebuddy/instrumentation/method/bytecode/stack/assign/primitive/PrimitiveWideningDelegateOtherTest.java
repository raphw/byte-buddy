package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class PrimitiveWideningDelegateOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSourceTypeThrowsException() throws Exception {
        PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(Object.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTargetTypeThrowsException() throws Exception {
        PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(int.class)).widenTo(new TypeDescription.ForLoadedType(Object.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(PrimitiveWideningDelegate.class).apply();
        ObjectPropertyAssertion.of(PrimitiveWideningDelegate.WideningStackManipulation.class).apply();
    }
}
