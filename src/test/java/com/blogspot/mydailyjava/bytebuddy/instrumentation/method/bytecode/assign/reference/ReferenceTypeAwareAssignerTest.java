package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.reference;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ReferenceTypeAwareAssignerTest {

    private static final String FOO = "foo";

    private MethodVisitor methodVisitor;
    private TypeDescription sourceTypeDescription;
    private TypeDescription targetTypeDescription;

    @Before
    public void setUp() throws Exception {
        methodVisitor = mock(MethodVisitor.class);
        sourceTypeDescription = mock(TypeDescription.class);
        targetTypeDescription = mock(TypeDescription.class);
    }

    @Test
    public void testMutualAssignable() throws Exception {
        configureAssignability(true, true);
        Assignment assignment = ReferenceTypeAwareAssigner.INSTANCE.assign(sourceTypeDescription, targetTypeDescription, false);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testSourceToTargetAssignable() throws Exception {
        configureAssignability(true, false);
        Assignment assignment = ReferenceTypeAwareAssigner.INSTANCE.assign(sourceTypeDescription, targetTypeDescription, false);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test(expected = IllegalStateException.class)
    public void testTargetToSourceAssignable() throws Exception {
        configureAssignability(false, true);
        Assignment assignment = ReferenceTypeAwareAssigner.INSTANCE.assign(sourceTypeDescription, targetTypeDescription, false);
        assertThat(assignment.isValid(), is(false));
        assignment.apply(methodVisitor);
    }

    @Test
    public void testTargetToSourceAssignableRuntimeType() throws Exception {
        configureAssignability(false, false);
        when(targetTypeDescription.getInternalName()).thenReturn(FOO);
        Assignment assignment = ReferenceTypeAwareAssigner.INSTANCE.assign(sourceTypeDescription, targetTypeDescription, true);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitTypeInsn(Opcodes.CHECKCAST, FOO);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testPrimitiveAssignabilityWhenEqual() throws Exception {
        TypeDescription primitiveType = new TypeDescription.ForLoadedType(int.class); // Cannot mock equals
        Assignment assignment = ReferenceTypeAwareAssigner.INSTANCE.assign(primitiveType, primitiveType, true);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test(expected = IllegalStateException.class)
    public void testPrimitiveAssignabilityWhenNotEqual() throws Exception {
        TypeDescription primitiveType = new TypeDescription.ForLoadedType(int.class);
        TypeDescription otherPrimitiveType = new TypeDescription.ForLoadedType(long.class);
        Assignment assignment = ReferenceTypeAwareAssigner.INSTANCE.assign(primitiveType, otherPrimitiveType, true);
        assertThat(assignment.isValid(), is(false));
        assignment.apply(methodVisitor);
    }

    private void configureAssignability(boolean sourceToTarget, boolean targetToSource) {
        if (sourceToTarget) {
            when(sourceTypeDescription.isAssignableTo(targetTypeDescription)).thenReturn(true);
            when(targetTypeDescription.isAssignableFrom(sourceTypeDescription)).thenReturn(true);
        }
        if (targetToSource) {
            when(targetTypeDescription.isAssignableTo(sourceTypeDescription)).thenReturn(true);
            when(sourceTypeDescription.isAssignableFrom(targetTypeDescription)).thenReturn(true);
        }
    }
}
