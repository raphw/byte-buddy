package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class DefaultValueTest {

    private MethodVisitor methodVisitor;
    private TypeDescription typeDescription;

    @Before
    public void setUp() throws Exception {
        methodVisitor = mock(MethodVisitor.class);
        typeDescription = mock(TypeDescription.class);
    }

    @Test
    public void testVoid() throws Exception {
        when(typeDescription.isPrimitive()).thenReturn(true);
        when(typeDescription.represents(void.class)).thenReturn(true);
        Assignment assignment = DefaultValue.load(typeDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testLong() throws Exception {
        when(typeDescription.isPrimitive()).thenReturn(true);
        when(typeDescription.represents(long.class)).thenReturn(true);
        Assignment assignment = DefaultValue.load(typeDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(2));
        assertThat(size.getMaximalSize(), is(2));
        verify(methodVisitor).visitInsn(Opcodes.LCONST_0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testFloat() throws Exception {
        when(typeDescription.isPrimitive()).thenReturn(true);
        when(typeDescription.represents(float.class)).thenReturn(true);
        Assignment assignment = DefaultValue.load(typeDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.FCONST_0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testDouble() throws Exception {
        when(typeDescription.isPrimitive()).thenReturn(true);
        when(typeDescription.represents(double.class)).thenReturn(true);
        Assignment assignment = DefaultValue.load(typeDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(2));
        assertThat(size.getMaximalSize(), is(2));
        verify(methodVisitor).visitInsn(Opcodes.DCONST_0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testNotSpecifiedPrimitive() throws Exception {
        when(typeDescription.isPrimitive()).thenReturn(true);
        Assignment assignment = DefaultValue.load(typeDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.ICONST_0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testReference() throws Exception {
        Assignment assignment = DefaultValue.load(typeDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.ACONST_NULL);
        verifyNoMoreInteractions(methodVisitor);
    }
}
