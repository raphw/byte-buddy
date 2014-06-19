package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class PrimitiveUnboxingDelegateOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSourceTypeThrowsException() throws Exception {
        PrimitiveUnboxingDelegate.forReferenceType(new TypeDescription.ForLoadedType(int.class));
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(PrimitiveUnboxingDelegate.forReferenceType(new TypeDescription.ForLoadedType(Object.class)).hashCode(),
                is(PrimitiveUnboxingDelegate.forReferenceType(new TypeDescription.ForLoadedType(Object.class)).hashCode()));
        assertThat(PrimitiveUnboxingDelegate.forReferenceType(new TypeDescription.ForLoadedType(Object.class)),
                is(PrimitiveUnboxingDelegate.forReferenceType(new TypeDescription.ForLoadedType(Object.class))));
        assertThat(PrimitiveUnboxingDelegate.forReferenceType(new TypeDescription.ForLoadedType(Object.class)).hashCode(),
                not(is(PrimitiveUnboxingDelegate.forReferenceType(new TypeDescription.ForLoadedType(String.class)).hashCode())));
        assertThat(PrimitiveUnboxingDelegate.forReferenceType(new TypeDescription.ForLoadedType(Object.class)),
                not(is(PrimitiveUnboxingDelegate.forReferenceType(new TypeDescription.ForLoadedType(String.class)))));
    }
}
