package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
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
public class PrimitiveNarrowingDelegateNontrivialTest {

    private final Class<?> sourceType;

    private final Class<?> targetType;

    private final int sizeChange;

    private final int[] opcodes;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription sourceTypeDescription, targetTypeDescription;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public PrimitiveNarrowingDelegateNontrivialTest(Class<?> sourceType,
                                                    Class<?> targetType,
                                                    int sizeChange,
                                                    int[] opcodes) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.sizeChange = sizeChange;
        this.opcodes = opcodes;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {byte.class, char.class, 0, new int[]{Opcodes.I2C}},
                {short.class, byte.class, 0, new int[]{Opcodes.I2B}},
                {short.class, char.class, 0, new int[]{Opcodes.I2C}},
                {char.class, byte.class, 0, new int[]{Opcodes.I2B}},
                {char.class, short.class, 0, new int[]{Opcodes.I2S}},
                {int.class, byte.class, 0, new int[]{Opcodes.I2B}},
                {int.class, short.class, 0, new int[]{Opcodes.I2S}},
                {int.class, char.class, 0, new int[]{Opcodes.I2C}},
                {long.class, byte.class, -1, new int[]{Opcodes.L2I, Opcodes.I2B}},
                {long.class, short.class, -1, new int[]{Opcodes.L2I, Opcodes.I2S}},
                {long.class, char.class, -1, new int[]{Opcodes.L2I, Opcodes.I2C}},
                {long.class, int.class, -1, new int[]{Opcodes.L2I}},
                {float.class, byte.class, 0, new int[]{Opcodes.F2I, Opcodes.I2B}},
                {float.class, short.class, 0, new int[]{Opcodes.F2I, Opcodes.I2S}},
                {float.class, char.class, 0, new int[]{Opcodes.F2I, Opcodes.I2C}},
                {float.class, int.class, 0, new int[]{Opcodes.F2I}},
                {float.class, long.class, 1, new int[]{Opcodes.F2L}},
                {double.class, byte.class, -1, new int[]{Opcodes.D2I, Opcodes.I2B}},
                {double.class, short.class, -1, new int[]{Opcodes.D2I, Opcodes.I2S}},
                {double.class, char.class, -1, new int[]{Opcodes.D2I, Opcodes.I2C}},
                {double.class, int.class, -1, new int[]{Opcodes.D2I}},
                {double.class, long.class, 0, new int[]{Opcodes.D2L}},
                {double.class, float.class, -1, new int[]{Opcodes.D2F}},
        });
    }

    @Before
    public void setUp() throws Exception {
        when(sourceTypeDescription.represents(sourceType)).thenReturn(true);
        when(targetTypeDescription.represents(targetType)).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(implementationContext);
    }

    @Test
    public void testNarrowingConversion() throws Exception {
        StackManipulation stackManipulation = PrimitiveNarrowingDelegate.forPrimitive(sourceTypeDescription).narrowTo(targetTypeDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(Math.max(0, sizeChange)));
        for (int opcode : opcodes) {
            verify(this.methodVisitor).visitInsn(opcode);
        }
        verifyNoMoreInteractions(this.methodVisitor);
    }
}
