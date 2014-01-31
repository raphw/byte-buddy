package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class MethodInvocationCallTest {

    private static final String FOO = "foo";
    private static final String BAR = "bar";
    private static final String BAZ = "baz";

    private static enum MethodType {
        STATIC(true, false, 0, Opcodes.INVOKESTATIC),
        INTERFACE(false, true, 1, Opcodes.INVOKEINTERFACE),
        VIRTUAL(false, false, 1, Opcodes.INVOKEVIRTUAL);

        private final boolean isStatic;
        private final boolean isInterface;
        private final int size;
        private final int opcode;

        private MethodType(boolean isStatic, boolean isInterface, int size, int opcode) {
            this.isStatic = isStatic;
            this.isInterface = isInterface;
            this.size = size;
            this.opcode = opcode;
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {3, TypeSize.SINGLE, MethodType.VIRTUAL},
                {10, TypeSize.SINGLE, MethodType.VIRTUAL},
                {0, TypeSize.DOUBLE, MethodType.VIRTUAL},
                {3, TypeSize.SINGLE, MethodType.INTERFACE},
                {3, TypeSize.SINGLE, MethodType.STATIC},
        });
    }

    private final int parameterSize;
    private final TypeSize returnTypeSize;
    private final MethodType methodType;

    public MethodInvocationCallTest(int parameterSize, TypeSize returnTypeSize, MethodType methodType) {
        this.parameterSize = parameterSize;
        this.returnTypeSize = returnTypeSize;
        this.methodType = methodType;
    }

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
        methodStackSize = parameterSize + methodType.size;
        when(methodDescription.getReturnType()).thenReturn(returnTypeDescription);
        when(returnTypeDescription.getStackSize()).thenReturn(returnTypeSize);
        when(methodDescription.getStackSize()).thenReturn(methodStackSize);
        when(methodDescription.isStatic()).thenReturn(methodType.isStatic);
        when(methodDescription.isDeclaredInInterface()).thenReturn(methodType.isInterface);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(typeDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
    }

    @Test
    public void testMethodInvocation() throws Exception {
        Assignment assignment = MethodInvocation.of(methodDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(returnTypeSize.getSize() - methodStackSize));
        assertThat(size.getMaximalSize(), is(Math.max(0, returnTypeSize.getSize() - methodStackSize)));
        verify(methodDescription, atLeast(1)).isStatic();
        verify(methodDescription, atLeast(1)).getStackSize();
        verify(methodDescription, atLeast(1)).getReturnType();
        verify(methodDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDeclaringType();
        verify(typeDescription, atLeast(1)).getInternalName();
        verify(methodDescription, atLeast(1)).getDescriptor();
        verify(methodVisitor).visitMethodInsn(methodType.opcode, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(typeDescription);
        verify(returnTypeDescription, atLeast(1)).getStackSize();
        verifyNoMoreInteractions(returnTypeDescription);
    }
}
