package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class PrimitiveUnboxingDelegateOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSourceTypeThrowsException() throws Exception {
        PrimitiveUnboxingDelegate.forReferenceType(new TypeDescription.ForLoadedType(int.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVoidIllegal() throws Exception {
        PrimitiveUnboxingDelegate.forPrimitive(new TypeDescription.ForLoadedType(void.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(PrimitiveUnboxingDelegate.class).apply();
        ObjectPropertyAssertion.of(PrimitiveUnboxingDelegate.ImplicitlyTypedUnboxingResponsible.class).apply();
        ObjectPropertyAssertion.of(PrimitiveUnboxingDelegate.ExplicitlyTypedUnboxingResponsible.class).apply();
    }
}
