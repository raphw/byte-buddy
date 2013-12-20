package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.utility.TypeSymbol;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodArgumentAssignment {

    INTEGER(Opcodes.ILOAD, 1),
    LONG(Opcodes.LLOAD, 2),
    FLOAT(Opcodes.FLOAD, 1),
    DOUBLE(Opcodes.DLOAD, 2),
    ARRAY(Opcodes.AALOAD, 1),
    REFERENCE(Opcodes.ALOAD, 1);

    public static MethodArgumentAssignment of(String name) {
        switch (name.charAt(0)) {
            case TypeSymbol.REFERENCE:
                return REFERENCE;
            case TypeSymbol.INT:
            case TypeSymbol.BOOLEAN:
            case TypeSymbol.BYTE:
            case TypeSymbol.CHAR:
            case TypeSymbol.SHORT:
                return INTEGER;
            case TypeSymbol.ARRAY:
                return ARRAY;
            case TypeSymbol.DOUBLE:
                return DOUBLE;
            case TypeSymbol.FLOAT:
                return FLOAT;
            case TypeSymbol.LONG:
                return LONG;
            default:
                throw new IllegalArgumentException("Illegal type: " + name);
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
        private final Assignment assignment;

        private ArgumentLoadingAssignment(int variableIndex, Assignment assignment) {
            this.variableIndex = variableIndex;
            this.assignment = assignment;
        }

        @Override
        public boolean isAssignable() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitVarInsn(loadOpcode, variableIndex);
            return assignment.apply(methodVisitor).withMaximum(operandStackSize);
        }
    }

    public Assignment applyTo(int variableIndex, Assignment assignment) {
        return new ArgumentLoadingAssignment(variableIndex, assignment);
    }
}

