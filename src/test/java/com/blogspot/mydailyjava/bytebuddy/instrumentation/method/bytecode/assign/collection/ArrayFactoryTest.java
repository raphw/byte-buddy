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

    private MethodVisitor methodVisitor;
    private Assignment assignment;

    @Before
    public void setUp() throws Exception {
        methodVisitor = mock(org.objectweb.asm.MethodVisitor.class);
        assignment = mock(Assignment.class);
        when(assignment.isValid()).thenReturn(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoArrayType() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typeDescription.isArray()).thenReturn(false);
        ArrayFactory.of(typeDescription);
    }

    @Test
    public void testPrimitiveCreationBoolean() throws Exception {
        testPrimitiveCreation(boolean[].class, Opcodes.T_BOOLEAN, Opcodes.BASTORE);
    }

    @Test
    public void testPrimitiveCreationByte() throws Exception {
        testPrimitiveCreation(byte[].class, Opcodes.T_BYTE, Opcodes.BASTORE);
    }

    @Test
    public void testPrimitiveCreationShort() throws Exception {
        testPrimitiveCreation(short[].class, Opcodes.T_SHORT, Opcodes.SASTORE);
    }

    @Test
    public void testPrimitiveCreationChar() throws Exception {
        testPrimitiveCreation(char[].class, Opcodes.T_CHAR, Opcodes.CASTORE);
    }

    @Test
    public void testPrimitiveCreationInt() throws Exception {
        testPrimitiveCreation(int[].class, Opcodes.T_INT, Opcodes.IASTORE);
    }

    @Test
    public void testPrimitiveCreationLong() throws Exception {
        testPrimitiveCreation(long[].class, Opcodes.T_LONG, Opcodes.LASTORE);
    }

    @Test
    public void testPrimitiveCreationFloat() throws Exception {
        testPrimitiveCreation(float[].class, Opcodes.T_FLOAT, Opcodes.FASTORE);
    }

    @Test
    public void testPrimitiveCreationDouble() throws Exception {
        testPrimitiveCreation(double[].class, Opcodes.T_DOUBLE, Opcodes.DASTORE);
    }

    private interface ArrayCreationVerifier {

        void verifyCreation();
    }

    private class Primitive implements ArrayCreationVerifier {

        private final int creationOpcode;

        private Primitive(int creationOpcode) {
            this.creationOpcode = creationOpcode;
        }

        @Override
        public void verifyCreation() {
            verify(methodVisitor).visitIntInsn(Opcodes.NEWARRAY, creationOpcode);
        }
    }

    private void testPrimitiveCreation(Class<?> type, int creationOpcode, int storageOpcode) throws Exception {
        testCreation(type, new Primitive(creationOpcode), storageOpcode);
    }

    private class Reference implements ArrayCreationVerifier {

        private final String typeName;

        private Reference(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public void verifyCreation() {
            verify(methodVisitor).visitTypeInsn(Opcodes.ANEWARRAY, typeName);
        }
    }

    @Test
    public void testReferenceCreation() throws Exception {
        testReferenceCreation(Object[].class);
    }

    @Test
    public void testNestedReferenceCreation() throws Exception {
        testReferenceCreation(Object[][].class);
    }

    private void testReferenceCreation(Class<?> type) throws Exception {
        testCreation(type, new Reference(Type.getInternalName(type.getComponentType())), Opcodes.AASTORE);
    }

    private void testCreation(Class<?> type, ArrayCreationVerifier arrayCreationVerifier, int storageOpcode) throws Exception {
        Assignment.Size elementSize = TypeSize.of(type.getComponentType()).toIncreasingSize();
        when(assignment.apply(any(MethodVisitor.class))).thenReturn(elementSize);
        ArrayFactory arrayFactory = ArrayFactory.of(new TypeDescription.ForLoadedType(type));
        Assignment arrayAssignment = arrayFactory.withValues(Arrays.<Assignment>asList(assignment));
        assertThat(arrayAssignment.isValid(), is(true));
        verify(assignment, atLeast(1)).isValid();
        Assignment.Size size = arrayAssignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(3 + elementSize.getSizeImpact()));
        verify(methodVisitor).visitInsn(Opcodes.ICONST_1);
        arrayCreationVerifier.verifyCreation();
        verify(methodVisitor).visitInsn(Opcodes.DUP);
        verify(methodVisitor).visitInsn(Opcodes.ICONST_0);
        verify(assignment).apply(methodVisitor);
        verify(methodVisitor).visitInsn(storageOpcode);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(assignment);
    }
}
