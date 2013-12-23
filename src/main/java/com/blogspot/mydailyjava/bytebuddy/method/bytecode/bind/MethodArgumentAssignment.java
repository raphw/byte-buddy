package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.utility.MethodDescriptor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodArgumentAssignment {

    INTEGER(Opcodes.ILOAD, 1),
    LONG(Opcodes.LLOAD, 2),
    FLOAT(Opcodes.FLOAD, 1),
    DOUBLE(Opcodes.DLOAD, 2),
    ARRAY_REFERENCE(Opcodes.AALOAD, 1),
    OBJECT_REFERENCE(Opcodes.ALOAD, 1);

    public static MethodArgumentAssignment of(String name) {
        switch (name.charAt(0)) {
            case MethodDescriptor.OBJECT_REFERENCE_SYMBOL:
                return OBJECT_REFERENCE;
            case MethodDescriptor.INT_SYMBOL:
            case MethodDescriptor.BOOLEAN_SYMBOL:
            case MethodDescriptor.BYTE_SYMBOL:
            case MethodDescriptor.CHAR_SYMBOL:
            case MethodDescriptor.SHORT_SYMBOL:
                return INTEGER;
            case MethodDescriptor.ARRAY_REFERENCE_SYMBOL:
                return ARRAY_REFERENCE;
            case MethodDescriptor.DOUBLE_SYMBOL:
                return DOUBLE;
            case MethodDescriptor.FLOAT_SYMBOL:
                return FLOAT;
            case MethodDescriptor.LONG_SYMBOL:
                return LONG;
            default:
                throw new IllegalArgumentException("Illegal method argument type: " + name);
        }
    }

    private final int loadOpcode;
    private final int operandStackSize;

    private MethodArgumentAssignment(int loadOpcode, int operandStackSize) {
        this.loadOpcode = loadOpcode;
        this.operandStackSize = operandStackSize;
    }

    private class ArgumentLoadingAssignment implements Assignment {

        private final int variableIndex;
        private final Assignment chainedAssignment;

        private ArgumentLoadingAssignment(int variableIndex, Assignment chainedAssignment) {
            this.variableIndex = variableIndex;
            this.chainedAssignment = chainedAssignment;
        }

        @Override
        public boolean isAssignable() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitVarInsn(loadOpcode, variableIndex);
            return chainedAssignment.apply(methodVisitor).aggregateLeftFirst(operandStackSize);
        }
    }

    public Assignment assignAt(int variableIndex, Assignment chainedAssignment) {
        return new ArgumentLoadingAssignment(variableIndex, chainedAssignment);
    }
}

