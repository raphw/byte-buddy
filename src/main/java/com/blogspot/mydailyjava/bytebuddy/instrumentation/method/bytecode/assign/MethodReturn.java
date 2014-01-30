package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodReturn implements Assignment {

    INTEGER(Opcodes.IRETURN, TypeSize.SINGLE),
    DOUBLE(Opcodes.DRETURN, TypeSize.DOUBLE),
    FLOAT(Opcodes.FRETURN, TypeSize.SINGLE),
    LONG(Opcodes.LRETURN, TypeSize.DOUBLE),
    VOID(Opcodes.RETURN, TypeSize.ZERO),
    ANY_REFERENCE(Opcodes.ARETURN, TypeSize.SINGLE);

    public static MethodReturn returning(TypeDescription typeDescription) {
        if (typeDescription.isPrimitive()) {
            if (typeDescription.represents(long.class)) {
                return LONG;
            } else if (typeDescription.represents(double.class)) {
                return DOUBLE;
            } else if (typeDescription.represents(float.class)) {
                return FLOAT;
            } else if (typeDescription.represents(void.class)) {
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
