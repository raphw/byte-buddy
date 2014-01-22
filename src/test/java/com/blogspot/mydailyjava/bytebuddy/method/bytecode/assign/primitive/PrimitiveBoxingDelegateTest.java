package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.LegalTrivialAssignment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

public class PrimitiveBoxingDelegateTest {

    private static final String VALUE_OF = "valueOf";

    private Assigner chainedAssigner;
    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        chainedAssigner = mock(Assigner.class);
        when(chainedAssigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(LegalTrivialAssignment.INSTANCE);
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testBoolean() throws Exception {
        Assignment assignment = PrimitiveBoxingDelegate.forPrimitive(boolean.class).assignBoxedTo(Void.class, chainedAssigner, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(chainedAssigner).assign(Boolean.class, Void.class, false);
        verifyNoMoreInteractions(chainedAssigner);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Boolean.class), VALUE_OF, "(Z)Ljava/lang/Boolean;");
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testByte() throws Exception {
        Assignment assignment = PrimitiveBoxingDelegate.forPrimitive(byte.class).assignBoxedTo(Void.class, chainedAssigner, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(chainedAssigner).assign(Byte.class, Void.class, false);
        verifyNoMoreInteractions(chainedAssigner);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Byte.class), VALUE_OF, "(B)Ljava/lang/Byte;");
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testShort() throws Exception {
        Assignment assignment = PrimitiveBoxingDelegate.forPrimitive(short.class).assignBoxedTo(Void.class, chainedAssigner, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(chainedAssigner).assign(Short.class, Void.class, false);
        verifyNoMoreInteractions(chainedAssigner);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Short.class), VALUE_OF, "(S)Ljava/lang/Short;");
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testChar() throws Exception {
        Assignment assignment = PrimitiveBoxingDelegate.forPrimitive(char.class).assignBoxedTo(Void.class, chainedAssigner, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(chainedAssigner).assign(Character.class, Void.class, false);
        verifyNoMoreInteractions(chainedAssigner);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Character.class), VALUE_OF, "(C)Ljava/lang/Character;");
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testInt() throws Exception {
        Assignment assignment = PrimitiveBoxingDelegate.forPrimitive(int.class).assignBoxedTo(Void.class, chainedAssigner, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(chainedAssigner).assign(Integer.class, Void.class, false);
        verifyNoMoreInteractions(chainedAssigner);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), VALUE_OF, "(I)Ljava/lang/Integer;");
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testLong() throws Exception {
        Assignment assignment = PrimitiveBoxingDelegate.forPrimitive(long.class).assignBoxedTo(Void.class, chainedAssigner, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-1));
        assertThat(size.getMaximalSize(), is(0));
        verify(chainedAssigner).assign(Long.class, Void.class, false);
        verifyNoMoreInteractions(chainedAssigner);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Long.class), VALUE_OF, "(J)Ljava/lang/Long;");
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testFloat() throws Exception {
        Assignment assignment = PrimitiveBoxingDelegate.forPrimitive(float.class).assignBoxedTo(Void.class, chainedAssigner, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(chainedAssigner).assign(Float.class, Void.class, false);
        verifyNoMoreInteractions(chainedAssigner);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), VALUE_OF, "(F)Ljava/lang/Float;");
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testDouble() throws Exception {
        Assignment assignment = PrimitiveBoxingDelegate.forPrimitive(double.class).assignBoxedTo(Void.class, chainedAssigner, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-1));
        assertThat(size.getMaximalSize(), is(0));
        verify(chainedAssigner).assign(Double.class, Void.class, false);
        verifyNoMoreInteractions(chainedAssigner);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Double.class), VALUE_OF, "(D)Ljava/lang/Double;");
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegal() throws Exception {
        PrimitiveBoxingDelegate.forPrimitive(Object.class);
    }
}
