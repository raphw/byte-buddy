package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodArgument {

    INTEGER(Opcodes.ILOAD, 5, TypeSize.SINGLE),
    LONG(Opcodes.LLOAD, 8, TypeSize.DOUBLE),
    FLOAT(Opcodes.FLOAD, 11, TypeSize.SINGLE),
    DOUBLE(Opcodes.DLOAD, 14, TypeSize.DOUBLE),
    OBJECT_REFERENCE(Opcodes.ALOAD, 17, TypeSize.SINGLE),
    ARRAY_REFERENCE(Opcodes.AALOAD, -1, TypeSize.SINGLE);

    public static MethodArgument forType(TypeDescription typeDescription) {
        if (typeDescription.isPrimitive()) {
            if (typeDescription.represents(long.class)) {
                return LONG;
            } else if (typeDescription.represents(double.class)) {
                return DOUBLE;
            } else if (typeDescription.represents(float.class)) {
                return FLOAT;
            } else if(typeDescription.represents(void.class)) {
                throw new IllegalArgumentException("Argument type cannot be void");
            } else {
                return INTEGER;
            }
        } else {
            if (typeDescription.isArray()) {
                return ARRAY_REFERENCE;
            } else {
                return OBJECT_REFERENCE;
            }
        }
    }

    private final int loadOpcode;
    private final int loadOpcodeShortcutIndex;
    private final Assignment.Size size;

    private MethodArgument(int loadOpcode, int loadOpcodeShortcutIndex, TypeSize typeSize) {
        this.loadOpcode = loadOpcode;
        this.loadOpcodeShortcutIndex = loadOpcodeShortcutIndex;
        this.size = typeSize.toIncreasingSize();
    }

    private class ArgumentLoadingAssignment implements Assignment {

        private final int variableIndex;

        private ArgumentLoadingAssignment(int variableIndex) {
            this.variableIndex = variableIndex;
        }

        @Override
        public boolean isValid() {
            return true;
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
            return size;
        }
    }

    public Assignment loadFromIndex(int variableIndex) {
        return new ArgumentLoadingAssignment(variableIndex);
    }
}

