package net.bytebuddy.instrumentation.type;

import org.junit.Test;

import static org.mockito.Mockito.mock;

public class TypeDescriptionArrayProjectionOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArity() throws Exception {
        TypeDescription.ArrayProjection.of(mock(TypeDescription.class), -1);
    }
}
