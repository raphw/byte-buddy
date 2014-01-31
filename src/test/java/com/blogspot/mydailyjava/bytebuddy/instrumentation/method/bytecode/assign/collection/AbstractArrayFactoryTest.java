package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.collection;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.mockito.asm.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public abstract class AbstractArrayFactoryTest {

    private TypeDescription typeDescription;
    private MethodVisitor methodVisitor;
    private Assignment assignment;

    @Before
    public void setUp() throws Exception {
        typeDescription = mock(TypeDescription.class);
        methodVisitor = mock(MethodVisitor.class);
        assignment = mock(Assignment.class);
        when(assignment.isValid()).thenReturn(true);
    }

    protected void testCreation(Class<?> componentType, int storageOpcode) throws Exception {
        prepareMocksFor(componentType);
        ArrayFactory arrayFactory = ArrayFactory.of(typeDescription);
        Assignment arrayAssignment = arrayFactory.withValues(Arrays.asList(assignment));
        assertThat(arrayAssignment.isValid(), is(true));
        verify(assignment, atLeast(1)).isValid();
        Assignment.Size size = arrayAssignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(3 + TypeSize.of(componentType).toIncreasingSize().getSizeImpact()));
        verify(methodVisitor).visitInsn(Opcodes.ICONST_1);
        verifyArrayCreation(methodVisitor);
        verify(methodVisitor).visitInsn(Opcodes.DUP);
        verify(methodVisitor).visitInsn(Opcodes.ICONST_0);
        verify(assignment).apply(methodVisitor);
        verify(methodVisitor).visitInsn(storageOpcode);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(assignment);
    }

    protected abstract void verifyArrayCreation(MethodVisitor methodVisitor);

    private void prepareMocksFor(Class<?> componentType) {
        when(typeDescription.isArray()).thenReturn(true);
        TypeDescription componentTypeDescription = mock(TypeDescription.class);
        when(typeDescription.getComponentType()).thenReturn(componentTypeDescription);
        when(componentTypeDescription.isPrimitive()).thenReturn(componentType.isPrimitive());
        when(componentTypeDescription.represents(componentType)).thenReturn(true);
        when(componentTypeDescription.getInternalName()).thenReturn(Type.getInternalName(componentType));
        when(componentTypeDescription.getStackSize()).thenReturn(TypeSize.of(componentType));
        when(assignment.apply(any(MethodVisitor.class))).thenReturn(TypeSize.of(componentType).toIncreasingSize());
    }
}
