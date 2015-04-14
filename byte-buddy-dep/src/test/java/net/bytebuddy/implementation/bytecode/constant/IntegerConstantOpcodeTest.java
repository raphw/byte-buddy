package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class IntegerConstantOpcodeTest {

    private final int value;

    private final int opcode;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public IntegerConstantOpcodeTest(int value, int opcode) {
        this.value = value;
        this.opcode = opcode;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {-1, Opcodes.ICONST_M1},
                {0, Opcodes.ICONST_0},
                {0, Opcodes.ICONST_0},
                {1, Opcodes.ICONST_1},
                {2, Opcodes.ICONST_2},
                {3, Opcodes.ICONST_3},
                {4, Opcodes.ICONST_4},
                {5, Opcodes.ICONST_5}
        });
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testConstant() throws Exception {
        StackManipulation loading = IntegerConstant.forValue(value);
        if (value == 0 || value == 1) {
            assertThat(loading, is(IntegerConstant.forValue(value == 1)));
        }
        assertThat(loading.isValid(), is(true));
        StackManipulation.Size size = loading.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(opcode);
        verifyNoMoreInteractions(methodVisitor);
    }
}
