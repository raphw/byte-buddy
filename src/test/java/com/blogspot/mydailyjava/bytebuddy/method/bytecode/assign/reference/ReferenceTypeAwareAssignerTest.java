package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.reference;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ReferenceTypeAwareAssignerTest {

    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testTrivialAssignment() throws Exception {
        Assignment assignment = ReferenceTypeAwareAssigner.INSTANCE.assign(Object.class, Object.class, false);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testUpcastAssignment() throws Exception {
        Assignment assignment = ReferenceTypeAwareAssigner.INSTANCE.assign(Integer.class, Object.class, false);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test(expected = IllegalStateException.class)
    public void testDowncastAssignmentWithoutRuntimeType() throws Exception {
        Assignment assignment = ReferenceTypeAwareAssigner.INSTANCE.assign(Object.class, Integer.class, false);
        assertThat(assignment.isValid(), is(false));
        assignment.apply(methodVisitor);
    }

    @Test
    public void testDowncastAssignmentWithRuntimeType() throws Exception {
        Assignment assignment = ReferenceTypeAwareAssigner.INSTANCE.assign(Object.class, Integer.class, true);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Integer.class));
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testTrivialPrimitiveAssignment() throws Exception {
        Assignment assignment = ReferenceTypeAwareAssigner.INSTANCE.assign(int.class, int.class, false);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonTrivialPrimitiveAssignment() throws Exception {
        Assignment assignment = ReferenceTypeAwareAssigner.INSTANCE.assign(int.class, long.class, false);
        assertThat(assignment.isValid(), is(false));
        assignment.apply(methodVisitor);
    }
}
