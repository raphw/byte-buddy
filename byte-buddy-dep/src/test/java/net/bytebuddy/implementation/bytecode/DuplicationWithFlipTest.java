package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
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
public class DuplicationWithFlipTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {StackSize.SINGLE, StackSize.SINGLE, Opcodes.DUP_X1},
                {StackSize.SINGLE, StackSize.DOUBLE, Opcodes.DUP_X2},
                {StackSize.DOUBLE, StackSize.SINGLE, Opcodes.DUP2_X1},
                {StackSize.DOUBLE, StackSize.DOUBLE, Opcodes.DUP2_X2}
        });
    }

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDefinition top, second;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    private final StackSize topSize, secondSize;

    private final int opcode;

    public DuplicationWithFlipTest(StackSize topSize, StackSize secondSize, int opcode) {
        this.topSize = topSize;
        this.secondSize = secondSize;
        this.opcode = opcode;
    }

    @Before
    public void setUp() throws Exception {
        when(top.getStackSize()).thenReturn(topSize);
        when(second.getStackSize()).thenReturn(secondSize);
    }

    @Test
    public void testFlip() throws Exception {
        StackManipulation stackManipulation = Duplication.of(top).flipOver(second);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getMaximalSize(), is(topSize.getSize()));
        assertThat(size.getSizeImpact(), is(topSize.getSize()));
        verify(methodVisitor).visitInsn(opcode);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }
}
