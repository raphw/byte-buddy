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
public class DoubleConstantOpcodeTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {0d, Opcodes.DCONST_0},
                {1d, Opcodes.DCONST_1}
        });
    }

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    private final double value;
    private final int opcode;

    public DoubleConstantOpcodeTest(double value, int opcode) {
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
        StackManipulation.Size size = DoubleConstant.forValue(value).apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(2));
        assertThat(size.getMaximalSize(), is(2));
        verify(methodVisitor).visitInsn(opcode);
        verifyNoMoreInteractions(methodVisitor);
    }
}
