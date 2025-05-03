package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(Parameterized.class)
public class PrimitiveComparisonTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {PrimitiveComparison.INTEGER_EQUALS, Opcodes.NOP, Opcodes.IF_ICMPNE, 1},
                {PrimitiveComparison.INTEGER_NOT_EQUALS, Opcodes.NOP, Opcodes.IF_ICMPEQ, 1},
                {PrimitiveComparison.INTEGER_LESS_THAN, Opcodes.NOP, Opcodes.IF_ICMPGE, 1},
                {PrimitiveComparison.INTEGER_LESS_THAN_OR_EQUALS, Opcodes.NOP, Opcodes.IF_ICMPGT, 1},
                {PrimitiveComparison.INTEGER_GREATER_THAN, Opcodes.NOP, Opcodes.IF_ICMPLE, 1},
                {PrimitiveComparison.INTEGER_GREATER_THAN_OR_EQUALS, Opcodes.NOP, Opcodes.IF_ICMPLT, 1},

                {PrimitiveComparison.LONG_EQUALS, Opcodes.LCMP, Opcodes.IFNE, 3},
                {PrimitiveComparison.LONG_NOT_EQUALS, Opcodes.LCMP, Opcodes.IFEQ, 3},
                {PrimitiveComparison.LONG_LESS_THAN, Opcodes.LCMP, Opcodes.IFGE, 3},
                {PrimitiveComparison.LONG_LESS_THAN_OR_EQUALS, Opcodes.LCMP, Opcodes.IFGT, 3},
                {PrimitiveComparison.LONG_GREATER_THAN, Opcodes.LCMP, Opcodes.IFLE, 3},
                {PrimitiveComparison.LONG_GREATER_THAN_OR_EQUALS, Opcodes.LCMP, Opcodes.IFLT, 3},

                {PrimitiveComparison.FLOAT_EQUALS, Opcodes.FCMPL, Opcodes.IFNE, 1},
                {PrimitiveComparison.FLOAT_NOT_EQUALS, Opcodes.FCMPL, Opcodes.IFEQ, 1},
                {PrimitiveComparison.FLOAT_LESS_THAN, Opcodes.FCMPG, Opcodes.IFGE, 1},
                {PrimitiveComparison.FLOAT_LESS_THAN_OR_EQUALS, Opcodes.FCMPG, Opcodes.IFGT, 1},
                {PrimitiveComparison.FLOAT_GREATER_THAN, Opcodes.FCMPL, Opcodes.IFLE, 1},
                {PrimitiveComparison.FLOAT_GREATER_THAN_OR_EQUALS, Opcodes.FCMPL, Opcodes.IFLT, 1},

                {PrimitiveComparison.DOUBLE_EQUALS, Opcodes.DCMPL, Opcodes.IFNE, 3},
                {PrimitiveComparison.DOUBLE_NOT_EQUALS, Opcodes.DCMPL, Opcodes.IFEQ, 3},
                {PrimitiveComparison.DOUBLE_LESS_THAN, Opcodes.DCMPG, Opcodes.IFGE, 3},
                {PrimitiveComparison.DOUBLE_LESS_THAN_OR_EQUALS, Opcodes.DCMPG, Opcodes.IFGT, 3},
                {PrimitiveComparison.DOUBLE_GREATER_THAN, Opcodes.DCMPL, Opcodes.IFLE, 3},
                {PrimitiveComparison.DOUBLE_GREATER_THAN_OR_EQUALS, Opcodes.DCMPL, Opcodes.IFLT, 3},
        });
    }

    private final StackManipulation stackManipulation;

    private final int opcodeCmp;

    private final int opcodeIf;

    private final int stackDecreasingSize;

    public PrimitiveComparisonTest(StackManipulation stackManipulation, int opcodeCmp, int opcodeIf, int stackDecreasingSize) {
        this.stackManipulation = stackManipulation;
        this.opcodeCmp = opcodeCmp;
        this.opcodeIf = opcodeIf;
        this.stackDecreasingSize = stackDecreasingSize;
    }

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Test
    public void testPrimitiveComparison() throws Exception {
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getMaximalSize(), is(0));
        assertThat(size.getSizeImpact(), is(-stackDecreasingSize));
        verifyCode();
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }

    private void verifyCode() {
        if (opcodeCmp != Opcodes.NOP) verify(methodVisitor).visitInsn(opcodeCmp);
        verify(methodVisitor).visitJumpInsn(eq(opcodeIf), any(Label.class));

        // then block
        verify(methodVisitor).visitInsn(Opcodes.ICONST_1);
        verify(methodVisitor).visitJumpInsn(eq(Opcodes.GOTO), any(Label.class));

        // else block
        verify(methodVisitor, times(2)).visitLabel(any(Label.class));
        verify(methodVisitor).visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        verify(methodVisitor).visitInsn(Opcodes.ICONST_0);

        verify(methodVisitor, times(2)).visitLabel(any(Label.class));
        verify(methodVisitor).visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
    }
}
