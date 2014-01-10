package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.ValueSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodArgument {

    INTEGER(Opcodes.ILOAD, 5, ValueSize.SINGLE),
    LONG(Opcodes.LLOAD, 8, ValueSize.DOUBLE),
    FLOAT(Opcodes.FLOAD, 11, ValueSize.SINGLE),
    DOUBLE(Opcodes.DLOAD, 14, ValueSize.DOUBLE),
    OBJECT_REFERENCE(Opcodes.ALOAD, 17, ValueSize.SINGLE),
    ARRAY_REFERENCE(Opcodes.AALOAD, -1, ValueSize.SINGLE);

    public static MethodArgument loading(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == long.class) {
                return LONG;
            } else if (type == double.class) {
                return DOUBLE;
            } else if (type == float.class) {
                return FLOAT;
            } else {
                return INTEGER;
            }
        } else {
            if (type.isArray()) {
                return ARRAY_REFERENCE;
            } else {
                return OBJECT_REFERENCE;
            }
        }
    }

    private final int loadOpcode;
    private final int loadOpcodeShortcutIndex;
    private final ValueSize valueSize;

    private MethodArgument(int loadOpcode, int loadOpcodeShortcutIndex, ValueSize valueSize) {
        this.loadOpcode = loadOpcode;
        this.loadOpcodeShortcutIndex = loadOpcodeShortcutIndex;
        this.valueSize = valueSize;
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
            return chainedAssignment.apply(methodVisitor).aggregateLeftFirst(valueSize.getSize());
        }
    }

    public Assignment loadFromIndex(int variableIndex, Assignment chainedAssignment) {
        return new ArgumentLoadingAssignment(variableIndex, chainedAssignment);
    }

    public Assignment.Size apply(int variableIndex, MethodVisitor methodVisitor) {
        return new ArgumentLoadingAssignment(variableIndex, LegalTrivialAssignment.INSTANCE).apply(methodVisitor);
    }
}

