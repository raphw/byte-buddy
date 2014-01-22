package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class VoidAwareAssignerTest {

    private Assigner assigner;
    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        assigner = mock(Assigner.class);
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testAssignVoidToVoid() throws Exception {
        Assigner voidAware = new VoidAwareAssigner(assigner, false);
        Assignment assignment = voidAware.assign(void.class, void.class, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(assigner);
        verifyZeroInteractions(methodVisitor);
    }

    @Test(expected = IllegalStateException.class)
    public void testAssignVoidToReferenceNoDefault() throws Exception {
        Assigner voidAware = new VoidAwareAssigner(assigner, false);
        Assignment assignment = voidAware.assign(void.class, String.class, false);
        assertThat(assignment.isAssignable(), is(false));
        assignment.apply(methodVisitor);
    }

    @Test
    public void testAssignVoidToReferenceDefault() throws Exception {
        Assigner voidAware = new VoidAwareAssigner(assigner, true);
        Assignment assignment = voidAware.assign(void.class, Object.class, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verifyZeroInteractions(assigner);
        verify(methodVisitor).visitInsn(Opcodes.ACONST_NULL);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test(expected = IllegalStateException.class)
    public void testAssignVoidToLongNoDefault() throws Exception {
        Assigner voidAware = new VoidAwareAssigner(assigner, false);
        Assignment assignment = voidAware.assign(void.class, String.class, false);
        assertThat(assignment.isAssignable(), is(false));
        assignment.apply(methodVisitor);
    }

    @Test
    public void testAssignVoidToLongDefault() throws Exception {
        Assigner voidAware = new VoidAwareAssigner(assigner, true);
        Assignment assignment = voidAware.assign(void.class, long.class, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(2));
        assertThat(size.getMaximalSize(), is(2));
        verifyZeroInteractions(assigner);
        verify(methodVisitor).visitInsn(Opcodes.LCONST_0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testAssignReferenceToVoid() throws Exception {
        Assigner voidAware = new VoidAwareAssigner(assigner, true);
        Assignment assignment = voidAware.assign(Object.class, void.class, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-1));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(assigner);
        verify(methodVisitor).visitInsn(Opcodes.POP);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testAssignLongToVoid() throws Exception {
        Assigner voidAware = new VoidAwareAssigner(assigner, true);
        Assignment assignment = voidAware.assign(long.class, void.class, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-2));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(assigner);
        verify(methodVisitor).visitInsn(Opcodes.POP2);
        verifyNoMoreInteractions(methodVisitor);
    }
}
