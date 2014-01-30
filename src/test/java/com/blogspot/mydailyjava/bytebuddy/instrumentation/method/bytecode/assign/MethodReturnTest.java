package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class MethodReturnTest {

    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testVoidReturn() throws Exception {
        Assignment assignment = MethodReturn.returning(new TypeDescription.ForLoadedType(void.class));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(Opcodes.RETURN);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testReferenceReturn() throws Exception {
        Assignment assignment = MethodReturn.returning(new TypeDescription.ForLoadedType(Object.class));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-1));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(Opcodes.ARETURN);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testLongReturn() throws Exception {
        Assignment assignment = MethodReturn.returning(new TypeDescription.ForLoadedType(long.class));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-2));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(Opcodes.LRETURN);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testDoubleReturn() throws Exception {
        Assignment assignment = MethodReturn.returning(new TypeDescription.ForLoadedType(double.class));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-2));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(Opcodes.DRETURN);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testFloatReturn() throws Exception {
        Assignment assignment = MethodReturn.returning(new TypeDescription.ForLoadedType(float.class));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-1));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(Opcodes.FRETURN);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testIntReturn() throws Exception {
        Assignment assignment = MethodReturn.returning(new TypeDescription.ForLoadedType(int.class));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-1));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(Opcodes.IRETURN);
        verifyNoMoreInteractions(methodVisitor);
    }
}
