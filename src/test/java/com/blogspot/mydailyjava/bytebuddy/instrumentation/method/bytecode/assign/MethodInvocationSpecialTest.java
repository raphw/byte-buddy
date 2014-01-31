package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class MethodInvocationSpecialTest {

    private static final String FOO = "foo";
    private static final String BAR = "bar";
    private static final String BAZ = "baz";

    private static final int PARAMETER_SIZE = 3;
    private static final TypeSize RETURN_TYPE_SIZE = TypeSize.SINGLE;

    private MethodDescription methodDescription;
    private TypeDescription typeDescription;
    private TypeDescription returnTypeDescription;
    private MethodVisitor methodVisitor;
    private int methodStackSize;

    @Before
    public void setUp() throws Exception {
        methodDescription = mock(MethodDescription.class);
        returnTypeDescription = mock(TypeDescription.class);
        typeDescription = mock(TypeDescription.class);
        methodVisitor = mock(MethodVisitor.class);
        methodStackSize = PARAMETER_SIZE + 1;
        when(methodDescription.getReturnType()).thenReturn(returnTypeDescription);
        when(returnTypeDescription.getStackSize()).thenReturn(RETURN_TYPE_SIZE);
        when(methodDescription.getStackSize()).thenReturn(methodStackSize);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(typeDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
    }

    @Test
    public void testMethodInvocation() throws Exception {
        when(methodDescription.isStatic()).thenReturn(false);
        when(methodDescription.isDeclaredInInterface()).thenReturn(false);
        Assignment assignment = MethodInvocation.special(methodDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(RETURN_TYPE_SIZE.getSize() - methodStackSize));
        assertThat(size.getMaximalSize(), is(Math.max(0, RETURN_TYPE_SIZE.getSize() - methodStackSize)));
        verify(methodDescription, atLeast(1)).isStatic();
        verify(methodDescription, atLeast(1)).getStackSize();
        verify(methodDescription, atLeast(1)).getReturnType();
        verify(methodDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDeclaringType();
        verify(typeDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDescriptor();
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(typeDescription);
        verify(returnTypeDescription, atLeast(1)).getStackSize();
        verifyNoMoreInteractions(returnTypeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMethodInvocationStatic() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        when(methodDescription.isDeclaredInInterface()).thenReturn(false);
        MethodInvocation.special(methodDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMethodInvocationInterface() throws Exception {
        when(methodDescription.isStatic()).thenReturn(false);
        when(methodDescription.isDeclaredInInterface()).thenReturn(true);
        MethodInvocation.special(methodDescription);
    }
}
