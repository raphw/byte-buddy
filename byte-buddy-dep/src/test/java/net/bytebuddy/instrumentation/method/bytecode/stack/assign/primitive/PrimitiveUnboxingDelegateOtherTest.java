package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class PrimitiveUnboxingDelegateOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSourceTypeThrowsException() throws Exception {
        PrimitiveUnboxingDelegate.forReferenceType(new TypeDescription.ForLoadedType(int.class));
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        ObjectPropertyAssertion.of(PrimitiveUnboxingDelegate.ImplicitlyTypedUnboxingResponsible.class).apply();
    }
}
