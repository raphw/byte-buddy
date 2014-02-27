package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(Parameterized.class)
public class FloatConstantOpcodeTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {0f, Opcodes.FCONST_0},
                {1f, Opcodes.FCONST_1},
                {2f, Opcodes.FCONST_2}
        });
    }

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    private final float value;
    private final int opcode;

    public FloatConstantOpcodeTest(float value, int opcode) {
        this.value = value;
        this.opcode = opcode;
    }

    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testConstant() throws Exception {
        StackManipulation.Size size = FloatConstant.forValue(value).apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(opcode);
        verifyNoMoreInteractions(methodVisitor);
    }
}
