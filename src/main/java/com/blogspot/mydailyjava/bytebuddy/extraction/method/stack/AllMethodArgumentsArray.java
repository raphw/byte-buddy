package com.blogspot.mydailyjava.bytebuddy.extraction.method.stack;

import com.blogspot.mydailyjava.bytebuddy.extraction.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.context.MethodContext;
import com.blogspot.mydailyjava.bytebuddy.utility.MethodDescriptorIterator;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AllMethodArgumentsArray implements CallStackArgument {

    private static final String OBJECT_TYPE = "Ljava/Lang/Object;";

    private static class AutoBoxingArrayFiller implements MethodDescriptorIterator.Visitor {

        private static final String VALUE_OF_METHOD_NAME = "valueOf";
        private static final String DOUBLE_WRAPPER_NAME = "java/lang/Double", DOUBLE_WRAPPER_DESCRIPTOR = "(D)Ljava/lang/Double;";
        private static final String FLOAT_WRAPPER_NAME = "java/lang/Float", FLOAT_WRAPPER_DESCRIPTOR = "(F)Ljava/lang/Float;";
        private static final String LONG_WRAPPER_NAME = "java/lang/Long", LONG_WRAPPER_DESCRIPTOR = "(J)Ljava/lang/Long;";
        private static final String INTEGER_WRAPPER_NAME = "java/lang/Integer", INTEGER_WRAPPER_DESCRIPTOR = "(I)Ljava/lang/Double;";
        private static final String CHAR_WRAPPER_NAME = "java/lang/Char", CHAR_WRAPPER_DESCRIPTOR = "(C)Ljava/lang/Char;";
        private static final String SHORT_WRAPPER_NAME = "java/lang/Short", SHORT_WRAPPER_DESCRIPTOR = "(S)Ljava/lang/Short;";
        private static final String BYTE_WRAPPER_NAME = "java/lang/Byte", BYTE_WRAPPER_DESCRIPTOR = "(B)Ljava/lang/Byte;";
        private static final String BOOLEAN_WRAPPER_NAME = "java/lang/Boolean", BOOLEAN_WRAPPER_DESCRIPTOR = "(Z)Ljava/lang/Boolean;";

        private final MethodVisitor methodVisitor;

        private int currentIndex;
        private Size currentSize;

        private AutoBoxingArrayFiller(MethodVisitor methodVisitor, Size initialSize) {
            this.methodVisitor = methodVisitor;
            this.currentSize = initialSize;
            this.currentIndex = 0;
        }

        private Size getCurrentSize() {
            return currentSize;
        }

        @Override
        public void visitObject(String substring, int localVariableIndex) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, currentIndex++);
            methodVisitor.visitIntInsn(Opcodes.ALOAD, localVariableIndex + 1);
            methodVisitor.visitInsn(Opcodes.AASTORE);
            currentSize = currentSize.merge(0, 3);
        }

        @Override
        public void visitArray(String substring, int localVariableIndex) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, currentIndex++);
            methodVisitor.visitIntInsn(Opcodes.AALOAD, localVariableIndex + 1);
            methodVisitor.visitInsn(Opcodes.AASTORE);
            currentSize = currentSize.merge(0, 3);
        }

        @Override
        public void visitDouble(int localVariableIndex) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, currentIndex++);
            methodVisitor.visitIntInsn(Opcodes.DLOAD, localVariableIndex + 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, DOUBLE_WRAPPER_NAME, VALUE_OF_METHOD_NAME, DOUBLE_WRAPPER_DESCRIPTOR);
            methodVisitor.visitInsn(Opcodes.AASTORE);
            currentSize = currentSize.merge(0, 4);
        }

        @Override
        public void visitLong(int localVariableIndex) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, currentIndex++);
            methodVisitor.visitIntInsn(Opcodes.LLOAD, localVariableIndex + 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, LONG_WRAPPER_NAME, VALUE_OF_METHOD_NAME, LONG_WRAPPER_DESCRIPTOR);
            methodVisitor.visitInsn(Opcodes.AASTORE);
            currentSize = currentSize.merge(0, 4);
        }

        @Override
        public void visitFloat(int localVariableIndex) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, currentIndex++);
            methodVisitor.visitIntInsn(Opcodes.FLOAD, localVariableIndex + 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, FLOAT_WRAPPER_NAME, VALUE_OF_METHOD_NAME, FLOAT_WRAPPER_DESCRIPTOR);
            methodVisitor.visitInsn(Opcodes.AASTORE);
            currentSize = currentSize.merge(0, 3);
        }

        @Override
        public void visitInt(int localVariableIndex) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, currentIndex++);
            methodVisitor.visitIntInsn(Opcodes.ILOAD, localVariableIndex + 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, INTEGER_WRAPPER_NAME, VALUE_OF_METHOD_NAME, INTEGER_WRAPPER_DESCRIPTOR);
            methodVisitor.visitInsn(Opcodes.AASTORE);
            currentSize = currentSize.merge(0, 3);
        }

        @Override
        public void visitBoolean(int localVariableIndex) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, currentIndex++);
            methodVisitor.visitIntInsn(Opcodes.ILOAD, localVariableIndex + 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, BOOLEAN_WRAPPER_NAME, VALUE_OF_METHOD_NAME, BOOLEAN_WRAPPER_DESCRIPTOR);
            methodVisitor.visitInsn(Opcodes.AASTORE);
            currentSize = currentSize.merge(0, 3);
        }

        @Override
        public void visitByte(int localVariableIndex) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, currentIndex++);
            methodVisitor.visitIntInsn(Opcodes.ILOAD, localVariableIndex + 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, BYTE_WRAPPER_NAME, VALUE_OF_METHOD_NAME, BYTE_WRAPPER_DESCRIPTOR);
            methodVisitor.visitInsn(Opcodes.AASTORE);
            currentSize = currentSize.merge(0, 3);
        }

        @Override
        public void visitChar(int localVariableIndex) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, currentIndex++);
            methodVisitor.visitIntInsn(Opcodes.ILOAD, localVariableIndex + 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, CHAR_WRAPPER_NAME, VALUE_OF_METHOD_NAME, CHAR_WRAPPER_DESCRIPTOR);
            methodVisitor.visitInsn(Opcodes.AASTORE);
            currentSize = currentSize.merge(0, 3);
        }

        @Override
        public void visitShort(int localVariableIndex) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, currentIndex++);
            methodVisitor.visitIntInsn(Opcodes.ILOAD, localVariableIndex + 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, SHORT_WRAPPER_NAME, VALUE_OF_METHOD_NAME, SHORT_WRAPPER_DESCRIPTOR);
            methodVisitor.visitInsn(Opcodes.AASTORE);
            currentSize = currentSize.merge(0, 3);
        }
    }

    private final String arrayTypeName;

    public AllMethodArgumentsArray() {
        this.arrayTypeName = OBJECT_TYPE;
    }

    public AllMethodArgumentsArray(String arrayTypeName) {
        this.arrayTypeName = arrayTypeName;
    }

    @Override
    public Size load(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext) {
        methodVisitor.visitIntInsn(Opcodes.BIPUSH, methodContext.getType().getArgumentTypes().length);
        methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, arrayTypeName);
        return new MethodDescriptorIterator(methodContext.getDescriptor()).apply(new AutoBoxingArrayFiller(methodVisitor, new Size(1, 1))).getCurrentSize();
    }
}
