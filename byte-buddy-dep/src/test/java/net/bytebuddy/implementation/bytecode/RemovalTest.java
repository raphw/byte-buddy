package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.implementation.Implementation;
import org.junit.After;
import org.junit.Before;
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
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class RemovalTest {

    private final StackSize stackSize;

    private final int opcode;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDefinition typeDefinition;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public RemovalTest(StackSize stackSize, int opcode) {
        this.stackSize = stackSize;
        this.opcode = opcode;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {StackSize.ZERO, Opcodes.NOP},
                {StackSize.SINGLE, Opcodes.POP},
                {StackSize.DOUBLE, Opcodes.POP2}
        });
    }

    @Before
    public void setUp() throws Exception {
        when(typeDefinition.getStackSize()).thenReturn(stackSize);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(implementationContext);
    }

    @Test
    public void testDuplication() throws Exception {
        StackManipulation stackManipulation = Removal.of(typeDefinition);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(-stackSize.getSize()));
        assertThat(size.getMaximalSize(), is(0));
        if (stackSize != StackSize.ZERO) {
            verify(methodVisitor).visitInsn(opcode);
        }
        verifyNoMoreInteractions(methodVisitor);
    }

}
