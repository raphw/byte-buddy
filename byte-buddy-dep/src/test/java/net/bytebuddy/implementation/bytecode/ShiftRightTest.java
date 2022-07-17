package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(Parameterized.class)
public class ShiftRightTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {ShiftRight.INTEGER, StackSize.SINGLE, Opcodes.ISHR, Opcodes.IUSHR},
                {ShiftRight.LONG, StackSize.DOUBLE, Opcodes.LSHR, Opcodes.LUSHR}
        });
    }

    private final ShiftRight shiftRight;

    private final StackSize stackSize;

    private final int signed;

    private final int unsigned;

    public ShiftRightTest(ShiftRight shiftRight, StackSize stackSize, int signed, int unsigned) {
        this.shiftRight = shiftRight;
        this.stackSize = stackSize;
        this.signed = signed;
        this.unsigned = unsigned;
    }

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Test
    public void testShiftRight() {
        StackManipulation.Size size = shiftRight.apply(methodVisitor, implementationContext);
        assertThat(size.getMaximalSize(), is(0));
        assertThat(size.getSizeImpact(), is(-stackSize.getSize()));
        verify(methodVisitor).visitInsn(signed);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }

    @Test
    public void testShiftRightUnsigned() {
        StackManipulation.Size size = shiftRight.toUnsigned().apply(methodVisitor, implementationContext);
        assertThat(size.getMaximalSize(), is(0));
        assertThat(size.getSizeImpact(), is(-stackSize.getSize()));
        verify(methodVisitor).visitInsn(unsigned);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }
}