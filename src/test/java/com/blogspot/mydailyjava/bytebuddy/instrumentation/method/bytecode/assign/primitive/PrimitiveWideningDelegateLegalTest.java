package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
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
        assertNoOpConversion(boolean.class, boolean.class);
    }

    @Test
    public void testByteToByte() throws Exception {
        assertNoOpConversion(byte.class, byte.class);
    }

    @Test
    public void testByteToShort() throws Exception {
        assertNoOpConversion(byte.class, short.class);
    }

    @Test
    public void testByteToInt() throws Exception {
        assertNoOpConversion(byte.class, int.class);
    }

    @Test
    public void testByteToLong() throws Exception {
        assertWideningConversion(byte.class, long.class, 1, Opcodes.I2L);
    }

    @Test
    public void testByteToFloat() throws Exception {
        assertWideningConversion(byte.class, float.class, 0, Opcodes.I2F);
    }

    @Test
    public void testByteToDouble() throws Exception {
        assertWideningConversion(byte.class, double.class, 1, Opcodes.I2L);
    }

    @Test
    public void testShortToShort() throws Exception {
        assertNoOpConversion(short.class, short.class);
    }

    @Test
    public void testShortToInt() throws Exception {
        assertNoOpConversion(short.class, int.class);
    }

    @Test
    public void testShortToLong() throws Exception {
        assertWideningConversion(short.class, long.class, 1, Opcodes.I2L);
    }

    @Test
    public void testShortToFloat() throws Exception {
        assertWideningConversion(short.class, float.class, 0, Opcodes.I2F);
    }

    @Test
    public void testShortToDouble() throws Exception {
        assertWideningConversion(short.class, double.class, 1, Opcodes.I2D);
    }

    @Test
    public void testCharToInt() throws Exception {
        assertNoOpConversion(char.class, int.class);
    }

    @Test
    public void testCharToChar() throws Exception {
        assertNoOpConversion(char.class, char.class);
    }

    @Test
    public void testCharToLong() throws Exception {
        assertWideningConversion(char.class, long.class, 1, Opcodes.I2L);
    }

    @Test
    public void testCharToFloat() throws Exception {
        assertWideningConversion(char.class, float.class, 0, Opcodes.I2F);
    }

    @Test
    public void testCharToDouble() throws Exception {
        assertWideningConversion(char.class, double.class, 1, Opcodes.I2D);
    }

    @Test
    public void testIntToInt() throws Exception {
        assertNoOpConversion(int.class, int.class);
    }

    @Test
    public void testIntToLong() throws Exception {
        assertWideningConversion(int.class, long.class, 1, Opcodes.I2L);
    }

    @Test
    public void testIntToFloat() throws Exception {
        assertWideningConversion(int.class, float.class, 0, Opcodes.I2F);
    }

    @Test
    public void testIntToDouble() throws Exception {
        assertWideningConversion(int.class, double.class, 1, Opcodes.I2D);
    }

    @Test
    public void testLongToLong() throws Exception {
        assertNoOpConversion(long.class, long.class);
    }

    @Test
    public void testLongToFloat() throws Exception {
        assertWideningConversion(long.class, float.class, -1, Opcodes.L2F);
    }

    @Test
    public void testLongToDouble() throws Exception {
        assertWideningConversion(long.class, double.class, 0, Opcodes.L2D);
    }

    @Test
    public void testFloatToFloat() throws Exception {
        assertNoOpConversion(float.class, float.class);
    }

    @Test
    public void testFloatToDouble() throws Exception {
        assertWideningConversion(float.class, double.class, 1, Opcodes.F2D);
    }

    @Test
    public void testDoubleToDouble() throws Exception {
        assertNoOpConversion(double.class, double.class);
    }

    private void assertNoOpConversion(Class<?> sourceType,
                                      Class<?> targetType) throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(sourceType))
                .widenTo(new TypeDescription.ForLoadedType(targetType));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    private void assertWideningConversion(Class<?> sourceType,
                                          Class<?> targetType,
                                          int sizeImpact,
                                          int opcode) throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(sourceType))
                .widenTo(new TypeDescription.ForLoadedType(targetType));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(sizeImpact));
        assertThat(size.getMaximalSize(), is(Math.max(0, sizeImpact)));
        verify(methodVisitor).visitInsn(opcode);
        verifyNoMoreInteractions(methodVisitor);
    }
}
