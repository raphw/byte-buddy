package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant.IntegerConstant;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class IntegerConstantTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation.Context instrumentationContext;

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testConstants() throws Exception {
        testConstant(0, Opcodes.ICONST_0);
        testConstant(1, Opcodes.ICONST_1);
        testConstant(2, Opcodes.ICONST_2);
        testConstant(3, Opcodes.ICONST_3);
        testConstant(4, Opcodes.ICONST_4);
        testConstant(5, Opcodes.ICONST_5);
    }

    private void testConstant(int value, int opcode) throws Exception {
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        StackManipulation.Size size = IntegerConstant.forValue(value).apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(opcode);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testBiPush() throws Exception {
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        StackManipulation.Size size = IntegerConstant.forValue(6).apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitIntInsn(Opcodes.BIPUSH, 6);
        verifyNoMoreInteractions(methodVisitor);
    }
}
