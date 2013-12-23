package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.method.utility.MethodDescriptor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodArgument {

    INTEGER(Opcodes.ILOAD, 5, 1),
    LONG(Opcodes.LLOAD, 8, 2),
    FLOAT(Opcodes.FLOAD, 11, 1),
    DOUBLE(Opcodes.DLOAD, 14, 2),
    OBJECT_REFERENCE(Opcodes.ALOAD, 17, 1),
    ARRAY_REFERENCE(Opcodes.AALOAD, -1, 1);

    public static MethodArgument forType(String name) {
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
    private final int loadOpcodeShortcutIndex;
    private final int operandStackSize;

    private MethodArgument(int loadOpcode, int loadOpcodeShortcutIndex, int operandStackSize) {
        this.loadOpcode = loadOpcode;
        this.loadOpcodeShortcutIndex = loadOpcodeShortcutIndex;
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
            return chainedAssignment.isAssignable();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            if (loadOpcodeShortcutIndex > -1) {
                switch (variableIndex) {
                    case 0:
                        methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutIndex);
                        break;
                    case 1:
                        methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutIndex + 1);
                        break;
                    case 2:
                        methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutIndex + 2);
                        break;
                    case 3:
                        methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutIndex + 3);
                        break;
                    default:
                        methodVisitor.visitVarInsn(loadOpcode, variableIndex);
                        break;
                }
            } else {
                methodVisitor.visitVarInsn(loadOpcode, variableIndex);
            }
            return chainedAssignment.apply(methodVisitor).aggregateLeftFirst(operandStackSize);
        }
    }

    public Assignment assignAt(int variableIndex, Assignment chainedAssignment) {
        return new ArgumentLoadingAssignment(variableIndex, chainedAssignment);
    }
}

