package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DuplicationOtherTest {

    @Test(expected = IllegalStateException.class)
    public void testZeroFlip() throws Exception {
        Duplication.ZERO.flipOver(mock(TypeDefinition.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSingleToZeroFlip() throws Exception {
        TypeDefinition typeDefinition = mock(TypeDefinition.class);
        when(typeDefinition.getStackSize()).thenReturn(StackSize.ZERO);
        Duplication.SINGLE.flipOver(typeDefinition);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoubleToZeroFlip() throws Exception {
        TypeDefinition typeDefinition = mock(TypeDefinition.class);
        when(typeDefinition.getStackSize()).thenReturn(StackSize.ZERO);
        Duplication.DOUBLE.flipOver(typeDefinition);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Duplication.class).apply();
        ObjectPropertyAssertion.of(Duplication.WithFlip.class).apply();
    }
}
