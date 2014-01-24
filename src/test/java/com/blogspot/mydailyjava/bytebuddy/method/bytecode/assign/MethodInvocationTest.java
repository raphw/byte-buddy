package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodInvocation;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodInvocationTest {

    private static final String FOO = "foo";
    private static final String BAR = "bar";
    private static final String BAZ = "baz";

    private MethodDescription methodDescription;
    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        methodDescription = mock(MethodDescription.class);
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testConcreteMethodWithSizeReduction() throws Exception {
        when(methodDescription.getParameterTypes()).thenReturn(new Class<?>[] {int.class, long.class});
        doReturn(void.class).when(methodDescription).getReturnType();
        when(methodDescription.isStatic()).thenReturn(false);
        when(methodDescription.isInterfaceMethod()).thenReturn(false);
        when(methodDescription.getDeclaringClassInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        Assignment assignment = MethodInvocation.of(methodDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-4));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodDescription, atLeast(1)).isStatic();
        verify(methodDescription, atLeast(1)).getParameterTypes();
        verify(methodDescription, atLeast(1)).getReturnType();
        verify(methodDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDeclaringClassInternalName();
        verify(methodDescription, atLeast(1)).getDescriptor();
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testConcreteMethodWithSizeIncrease() throws Exception {
        when(methodDescription.getParameterTypes()).thenReturn(new Class<?>[0]);
        doReturn(double.class).when(methodDescription).getReturnType();
        when(methodDescription.isStatic()).thenReturn(false);
        when(methodDescription.isInterfaceMethod()).thenReturn(false);
        when(methodDescription.getDeclaringClassInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        Assignment assignment = MethodInvocation.of(methodDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodDescription, atLeast(1)).isStatic();
        verify(methodDescription, atLeast(1)).getParameterTypes();
        verify(methodDescription, atLeast(1)).getReturnType();
        verify(methodDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDeclaringClassInternalName();
        verify(methodDescription, atLeast(1)).getDescriptor();
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testInterfaceMethod() throws Exception {
        when(methodDescription.getParameterTypes()).thenReturn(new Class<?>[] {int.class, long.class});
        doReturn(void.class).when(methodDescription).getReturnType();
        when(methodDescription.isStatic()).thenReturn(false);
        when(methodDescription.isInterfaceMethod()).thenReturn(true);
        when(methodDescription.getDeclaringClassInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        Assignment assignment = MethodInvocation.of(methodDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-4));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodDescription, atLeast(1)).isStatic();
        verify(methodDescription, atLeast(1)).getParameterTypes();
        verify(methodDescription, atLeast(1)).getReturnType();
        verify(methodDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDeclaringClassInternalName();
        verify(methodDescription, atLeast(1)).getDescriptor();
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEINTERFACE, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testStaticMethod() throws Exception {
        when(methodDescription.getParameterTypes()).thenReturn(new Class<?>[] {int.class, long.class});
        doReturn(void.class).when(methodDescription).getReturnType();
        when(methodDescription.isStatic()).thenReturn(true);
        when(methodDescription.isInterfaceMethod()).thenReturn(false);
        when(methodDescription.getDeclaringClassInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        Assignment assignment = MethodInvocation.of(methodDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-3));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodDescription, atLeast(1)).isStatic();
        verify(methodDescription, atLeast(1)).getParameterTypes();
        verify(methodDescription, atLeast(1)).getReturnType();
        verify(methodDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDeclaringClassInternalName();
        verify(methodDescription, atLeast(1)).getDescriptor();
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
    }
}
