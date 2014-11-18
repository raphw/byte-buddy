package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.After;
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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class DefaultValueTest {

    private final Class<?> type;
    private final int opcode;
    @Rule
    public TestRule mockitoRule = new MockitoRule(this);
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private TypeDescription typeDescription;
    @Mock
    private Instrumentation.Context instrumentationContext;

    public DefaultValueTest(Class<?> type, int opcode) {
        this.type = type;
        this.opcode = opcode;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {void.class, -1},
                {boolean.class, Opcodes.ICONST_0},
                {byte.class, Opcodes.ICONST_0},
                {short.class, Opcodes.ICONST_0},
                {char.class, Opcodes.ICONST_0},
                {int.class, Opcodes.ICONST_0},
                {long.class, Opcodes.LCONST_0},
                {float.class, Opcodes.FCONST_0},
                {double.class, Opcodes.DCONST_0},
                {Object.class, Opcodes.ACONST_NULL}
        });
    }

    @Before
    public void setUp() throws Exception {
        when(typeDescription.isPrimitive()).thenReturn(type.isPrimitive());
        when(typeDescription.represents(type)).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testDefaultValue() throws Exception {
        StackManipulation stackManipulation = DefaultValue.of(typeDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(StackSize.of(type).getSize()));
        assertThat(size.getMaximalSize(), is(StackSize.of(type).getSize()));
        if (opcode == -1) {
            verifyZeroInteractions(methodVisitor);
        } else {
            verify(methodVisitor).visitInsn(opcode);
            verifyNoMoreInteractions(methodVisitor);
        }
    }
}
