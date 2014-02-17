package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodArgument {

    INTEGER(Opcodes.ILOAD, 5, StackSize.SINGLE),
    LONG(Opcodes.LLOAD, 8, StackSize.DOUBLE),
    FLOAT(Opcodes.FLOAD, 11, StackSize.SINGLE),
    DOUBLE(Opcodes.DLOAD, 14, StackSize.DOUBLE),
    OBJECT_REFERENCE(Opcodes.ALOAD, 17, StackSize.SINGLE),
    ARRAY_REFERENCE(Opcodes.AALOAD, -1, StackSize.SINGLE);

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
    private final StackManipulation.Size size;

    private MethodArgument(int loadOpcode, int loadOpcodeShortcutIndex, StackSize stackSize) {
        this.loadOpcode = loadOpcode;
        this.loadOpcodeShortcutIndex = loadOpcodeShortcutIndex;
        this.size = stackSize.toIncreasingSize();
    }

    private class ArgumentLoadingStackManipulation implements StackManipulation {

        private final int variableIndex;

        private ArgumentLoadingStackManipulation(int variableIndex) {
            this.variableIndex = variableIndex;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
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

    public StackManipulation loadFromIndex(int variableIndex) {
        return new ArgumentLoadingStackManipulation(variableIndex);
    }
}

