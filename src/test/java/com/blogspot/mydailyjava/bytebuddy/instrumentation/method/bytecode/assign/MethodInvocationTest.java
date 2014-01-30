package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
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
    private TypeDescription typeDescription;
    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        methodDescription = mock(MethodDescription.class);
        typeDescription = mock(TypeDescription.class);
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testConcreteMethodWithSizeReduction() throws Exception {
        when(methodDescription.getParameterTypes())
                .thenReturn(new TypeList.ForLoadedType(new Class<?>[]{int.class, long.class}));
        when(methodDescription.getParameterSize()).thenReturn(4);
        when(methodDescription.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(void.class));
        when(methodDescription.isStatic()).thenReturn(false);
        when(methodDescription.isDeclaredInInterface()).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(typeDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        Assignment assignment = MethodInvocation.of(methodDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-4));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodDescription, atLeast(1)).isStatic();
        verify(methodDescription, atLeast(1)).getParameterSize();
        verify(methodDescription, atLeast(1)).getReturnType();
        verify(methodDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDeclaringType();
        verify(typeDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDescriptor();
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(typeDescription);
    }

    @Test
    public void testConcreteMethodWithSizeIncrease() throws Exception {
        when(methodDescription.getParameterTypes())
                .thenReturn(new TypeList.ForLoadedType(new Class<?>[0]));
        when(methodDescription.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(double.class));
        when(methodDescription.getParameterSize()).thenReturn(1);
        when(methodDescription.isStatic()).thenReturn(false);
        when(methodDescription.isDeclaredInInterface()).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(typeDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        Assignment assignment = MethodInvocation.of(methodDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodDescription, atLeast(1)).isStatic();
        verify(methodDescription, atLeast(1)).getParameterSize();
        verify(methodDescription, atLeast(1)).getReturnType();
        verify(methodDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDeclaringType();
        verify(typeDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDescriptor();
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(typeDescription);
    }

    @Test
    public void testInterfaceMethod() throws Exception {
        when(methodDescription.getParameterTypes())
                .thenReturn(new TypeList.ForLoadedType(new Class<?>[]{int.class, long.class}));
        when(methodDescription.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(void.class));
        when(methodDescription.isStatic()).thenReturn(false);
        when(methodDescription.getParameterSize()).thenReturn(4);
        when(methodDescription.isDeclaredInInterface()).thenReturn(true);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(typeDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        Assignment assignment = MethodInvocation.of(methodDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-4));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodDescription, atLeast(1)).isStatic();
        verify(methodDescription, atLeast(1)).getParameterSize();
        verify(methodDescription, atLeast(1)).getReturnType();
        verify(methodDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDeclaringType();
        verify(typeDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDescriptor();
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEINTERFACE, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(typeDescription);
    }

    @Test
    public void testStaticMethod() throws Exception {
        when(methodDescription.getParameterTypes()).thenReturn(
                new TypeList.ForLoadedType(new Class<?>[]{int.class, long.class}));
        when(methodDescription.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(void.class));
        when(methodDescription.isStatic()).thenReturn(true);
        when(methodDescription.getParameterSize()).thenReturn(3);
        when(methodDescription.isDeclaredInInterface()).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(typeDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        Assignment assignment = MethodInvocation.of(methodDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-3));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodDescription, atLeast(1)).isStatic();
        verify(methodDescription, atLeast(1)).getParameterSize();
        verify(methodDescription, atLeast(1)).getReturnType();
        verify(methodDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDeclaringType();
        verify(typeDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDescriptor();
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(typeDescription);
    }

    @Test
    public void testCallMethodInvokeSpecial() throws Exception {
        when(methodDescription.getParameterTypes())
                .thenReturn(new TypeList.ForLoadedType(new Class<?>[]{int.class, long.class}));
        when(methodDescription.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(void.class));
        when(methodDescription.isStatic()).thenReturn(false);
        when(methodDescription.getParameterSize()).thenReturn(4);
        when(methodDescription.isDeclaredInInterface()).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(typeDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        Assignment assignment = MethodInvocation.special(methodDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-4));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodDescription, atLeast(1)).isStatic();
        verify(methodDescription, atLeast(1)).getParameterSize();
        verify(methodDescription, atLeast(1)).getReturnType();
        verify(methodDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDeclaringType();
        verify(typeDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDescriptor();
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(typeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallMethodInvokeSpecialOnStatic() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        MethodInvocation.special(methodDescription);
    }
}
