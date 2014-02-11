package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class MethodInvocationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";
    private static final int SIZE_CHANGE = 3;

    private MethodDescription methodDescription;
    private TypeDescription typeDescription;
    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        methodDescription = mock(MethodDescription.class);
        methodVisitor = mock(MethodVisitor.class);
        TypeDescription returnType = mock(TypeDescription.class);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(returnType.getStackSize()).thenReturn(TypeSize.SINGLE);
        when(methodDescription.getStackSize()).thenReturn(SIZE_CHANGE + 1);
        typeDescription = mock(TypeDescription.class);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(typeDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(QUX);
    }

    @Test
    public void testStatic() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        Assignment assignment = MethodInvocation.invoke(methodDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-1 * SIZE_CHANGE));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, FOO, BAR, QUX);
        verify(methodDescription, atLeast(1)).isStatic();
    }


    @Test
    @Ignore("Wait til API was refactored, required general test refactoring, the mocking effort became to heavy and the tests to strict")
    public void testRewriteTests() throws Exception {
        fail("Add more tests!");
    }
}
