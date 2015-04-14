package net.bytebuddy.implementation.bytecode.collection;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class ArrayAccessOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testVoidThrowsException() throws Exception {
        ArrayAccess.of(new TypeDescription.ForLoadedType(void.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ArrayAccess.class).apply();
        ObjectPropertyAssertion.of(ArrayAccess.Loader.class).apply();
        ObjectPropertyAssertion.of(ArrayAccess.Putter.class).apply();
    }
}
