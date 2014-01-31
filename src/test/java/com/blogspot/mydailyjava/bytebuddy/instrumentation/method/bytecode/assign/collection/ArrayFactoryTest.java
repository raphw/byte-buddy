package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.collection;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class ArrayFactoryTest {

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

    @Test(expected = IllegalArgumentException.class)
    public void testNoArrayType() throws Exception {
        when(typeDescription.isArray()).thenReturn(false);
        ArrayFactory.of(typeDescription);
    }

    @Test
    public void testPrimitiveCreationBoolean() throws Exception {
        testPrimitiveCreation(boolean.class, Opcodes.T_BOOLEAN, Opcodes.BASTORE);
    }

    @Test
    public void testPrimitiveCreationByte() throws Exception {
        testPrimitiveCreation(byte.class, Opcodes.T_BYTE, Opcodes.BASTORE);
    }

    @Test
    public void testPrimitiveCreationShort() throws Exception {
        testPrimitiveCreation(short.class, Opcodes.T_SHORT, Opcodes.SASTORE);
    }

    @Test
    public void testPrimitiveCreationChar() throws Exception {
        testPrimitiveCreation(char.class, Opcodes.T_CHAR, Opcodes.CASTORE);
    }

    @Test
    public void testPrimitiveCreationInt() throws Exception {
        testPrimitiveCreation(int.class, Opcodes.T_INT, Opcodes.IASTORE);
    }

    @Test
    public void testPrimitiveCreationLong() throws Exception {
        testPrimitiveCreation(long.class, Opcodes.T_LONG, Opcodes.LASTORE);
    }

    @Test
    public void testPrimitiveCreationFloat() throws Exception {
        testPrimitiveCreation(float.class, Opcodes.T_FLOAT, Opcodes.FASTORE);
    }

    @Test
    public void testPrimitiveCreationDouble() throws Exception {
        testPrimitiveCreation(double.class, Opcodes.T_DOUBLE, Opcodes.DASTORE);
    }

    private interface ArrayCreationVerifier {

        void verifyCreation();
    }

    private class PrimitiveVerifier implements ArrayCreationVerifier {

        private final int creationOpcode;

        private PrimitiveVerifier(int creationOpcode) {
            this.creationOpcode = creationOpcode;
        }

        @Override
        public void verifyCreation() {
            verify(methodVisitor).visitIntInsn(Opcodes.NEWARRAY, creationOpcode);
        }
    }

    private void testPrimitiveCreation(Class<?> componentType, int creationOpcode, int storageOpcode) throws Exception {
        testCreation(componentType, new PrimitiveVerifier(creationOpcode), storageOpcode);
    }

    private class ReferenceVerifier implements ArrayCreationVerifier {

        private final String typeName;

        private ReferenceVerifier(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public void verifyCreation() {
            verify(methodVisitor).visitTypeInsn(Opcodes.ANEWARRAY, typeName);
        }
    }

    @Test
    public void testReferenceCreation() throws Exception {
        testReferenceCreation(Object.class);
    }

    @Test
    public void testNestedReferenceCreation() throws Exception {
        testReferenceCreation(Object[].class);
    }

    private void testReferenceCreation(Class<?> componentType) throws Exception {
        testCreation(componentType, new ReferenceVerifier(Type.getInternalName(componentType)), Opcodes.AASTORE);
    }

    private void testCreation(Class<?> componentType, ArrayCreationVerifier arrayCreationVerifier, int storageOpcode) throws Exception {
        prepareMocksFor(componentType);
        ArrayFactory arrayFactory = ArrayFactory.of(typeDescription);
        Assignment arrayAssignment = arrayFactory.withValues(Arrays.asList(assignment));
        assertThat(arrayAssignment.isValid(), is(true));
        verify(assignment, atLeast(1)).isValid();
        Assignment.Size size = arrayAssignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(3 + TypeSize.of(componentType).toIncreasingSize().getSizeImpact()));
        verify(methodVisitor).visitInsn(Opcodes.ICONST_1);
        arrayCreationVerifier.verifyCreation();
        verify(methodVisitor).visitInsn(Opcodes.DUP);
        verify(methodVisitor).visitInsn(Opcodes.ICONST_0);
        verify(assignment).apply(methodVisitor);
        verify(methodVisitor).visitInsn(storageOpcode);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(assignment);
    }

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
