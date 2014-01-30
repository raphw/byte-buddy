package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class IntegerConstantTest {

    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testConstant() throws Exception {
        testConstant(0, Opcodes.ICONST_0);
        testConstant(1, Opcodes.ICONST_1);
        testConstant(2, Opcodes.ICONST_2);
        testConstant(3, Opcodes.ICONST_3);
        testConstant(4, Opcodes.ICONST_4);
        testConstant(5, Opcodes.ICONST_5);
    }

    private void testConstant(int value, int opcode) throws Exception {
        Assignment.Size size = IntegerConstant.forValue(value).apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(opcode);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testBiPush() throws Exception {
        Assignment.Size size = IntegerConstant.forValue(6).apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitIntInsn(Opcodes.BIPUSH, 6);
        verifyNoMoreInteractions(methodVisitor);
    }
}
