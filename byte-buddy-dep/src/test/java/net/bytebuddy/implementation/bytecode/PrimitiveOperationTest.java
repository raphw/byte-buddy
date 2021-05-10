package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrimitiveOperationTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Addition.INTEGER, StackSize.SINGLE, Opcodes.IADD},
                {Addition.LONG, StackSize.DOUBLE, Opcodes.LADD},
                {Addition.FLOAT, StackSize.SINGLE, Opcodes.FADD},
                {Addition.DOUBLE, StackSize.DOUBLE, Opcodes.DADD},
        
                {Substraction.INTEGER, StackSize.SINGLE, Opcodes.ISUB},
                {Substraction.LONG, StackSize.DOUBLE, Opcodes.LSUB},
                {Substraction.FLOAT, StackSize.SINGLE, Opcodes.FSUB},
                {Substraction.DOUBLE, StackSize.DOUBLE, Opcodes.DSUB},
        
                {Multiplication.INTEGER, StackSize.SINGLE, Opcodes.IMUL},
                {Multiplication.LONG, StackSize.DOUBLE, Opcodes.LMUL},
                {Multiplication.FLOAT, StackSize.SINGLE, Opcodes.FMUL},
                {Multiplication.DOUBLE, StackSize.DOUBLE, Opcodes.DMUL},
        
                {Division.INTEGER, StackSize.SINGLE, Opcodes.IDIV},
                {Division.LONG, StackSize.DOUBLE, Opcodes.LDIV},
                {Division.FLOAT, StackSize.SINGLE, Opcodes.FDIV},
                {Division.DOUBLE, StackSize.DOUBLE, Opcodes.DDIV},
        
                {Remainder.INTEGER, StackSize.SINGLE, Opcodes.IREM},
                {Remainder.LONG, StackSize.DOUBLE, Opcodes.LREM},
                {Remainder.FLOAT, StackSize.SINGLE, Opcodes.FREM},
                {Remainder.DOUBLE, StackSize.DOUBLE, Opcodes.DREM},
        
                {ShiftLeft.INTEGER, StackSize.SINGLE, Opcodes.ISHL},
                {ShiftLeft.LONG, StackSize.DOUBLE, Opcodes.LSHL},
                {ShiftRight.INTEGER, StackSize.SINGLE, Opcodes.ISHR},
                {ShiftRight.LONG, StackSize.DOUBLE, Opcodes.LSHR},
                {ShiftRightUnsigned.INTEGER, StackSize.SINGLE, Opcodes.IUSHR},
                {ShiftRightUnsigned.LONG, StackSize.DOUBLE, Opcodes.LUSHR},
        });
    }

    private final StackManipulation stackManipulation;

    private final StackSize stackSize;

    private final int opcodes;

    public PrimitiveOperationTest(StackManipulation stackManipulation, StackSize stackSize, int opcodes) {
        this.stackManipulation = stackManipulation;
        this.stackSize = stackSize;
        this.opcodes = opcodes;
    }

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Test
    public void testAddition() {
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getMaximalSize(), is(0));
        assertThat(size.getSizeImpact(), is(-stackSize.getSize()));
        verify(methodVisitor).visitInsn(opcodes);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }
}