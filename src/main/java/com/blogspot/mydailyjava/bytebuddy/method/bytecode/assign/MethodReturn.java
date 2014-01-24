package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodReturn implements Assignment {

    INTEGER(Opcodes.IRETURN, TypeSize.SINGLE),
    DOUBLE(Opcodes.DRETURN, TypeSize.DOUBLE),
    FLOAT(Opcodes.FRETURN, TypeSize.SINGLE),
    LONG(Opcodes.LRETURN, TypeSize.DOUBLE),
    VOID(Opcodes.RETURN, TypeSize.NONE),
    ANY_REFERENCE(Opcodes.ARETURN, TypeSize.SINGLE);

    public static MethodReturn returning(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == long.class) {
                return LONG;
            } else if (type == double.class) {
                return DOUBLE;
            } else if (type == float.class) {
                return FLOAT;
            } else if (type == void.class) {
                return VOID;
            } else {
                return INTEGER;
            }
        } else {
            return ANY_REFERENCE;
        }
    }

    private final int returnOpcode;
    private final TypeSize typeSize;

    private MethodReturn(int returnOpcode, TypeSize typeSize) {
        this.returnOpcode = returnOpcode;
        this.typeSize = typeSize;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(returnOpcode);
        return typeSize.toDecreasingSize();
    }
}
