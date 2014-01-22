package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PrimitiveWideningDelegateLegalTest {

    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testBooleanToBoolean() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(boolean.class).widenTo(boolean.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testByteToByte() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(byte.class).widenTo(byte.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testByteToShort() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(byte.class).widenTo(short.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testByteToInt() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(byte.class).widenTo(int.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testByteToLong() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(byte.class).widenTo(long.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.I2L);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testByteToFloat() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(byte.class).widenTo(float.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(Opcodes.I2F);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testByteToDouble() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(byte.class).widenTo(double.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.I2L);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testShortToShort() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(short.class).widenTo(short.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testShortToInt() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(short.class).widenTo(int.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testShortToLong() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(short.class).widenTo(long.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.I2L);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testShortToFloat() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(short.class).widenTo(float.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(Opcodes.I2F);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testShortToDouble() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(short.class).widenTo(double.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.I2D);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testCharToInt() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(char.class).widenTo(char.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testCharToChar() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(char.class).widenTo(char.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testCharToLong() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(char.class).widenTo(long.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.I2L);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testCharToFloat() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(char.class).widenTo(float.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(Opcodes.I2F);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testCharToDouble() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(char.class).widenTo(double.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.I2D);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testIntToInt() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(int.class).widenTo(int.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testIntToLong() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(int.class).widenTo(long.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.I2L);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testIntToFloat() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(int.class).widenTo(float.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(Opcodes.I2F);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testIntToDouble() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(int.class).widenTo(double.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.I2D);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testLongToLong() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(long.class).widenTo(long.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testLongToFloat() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(long.class).widenTo(float.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-1));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(Opcodes.L2F);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testLongToDouble() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(long.class).widenTo(double.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(Opcodes.L2D);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testFloatToFloat() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(float.class).widenTo(float.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testFloatToDouble() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(float.class).widenTo(double.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.F2L);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testDoubleToDouble() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(double.class).widenTo(double.class);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }
}
