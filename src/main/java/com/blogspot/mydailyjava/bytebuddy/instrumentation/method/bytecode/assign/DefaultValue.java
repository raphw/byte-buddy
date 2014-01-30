package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum DefaultValue implements Assignment {

    INTEGER(Opcodes.ICONST_0, TypeSize.SINGLE),
    LONG(Opcodes.LCONST_0, TypeSize.DOUBLE),
    FLOAT(Opcodes.FCONST_0, TypeSize.SINGLE),
    DOUBLE(Opcodes.DCONST_0, TypeSize.DOUBLE),
    VOID(-1, TypeSize.ZERO),
    ANY_REFERENCE(Opcodes.ACONST_NULL, TypeSize.SINGLE);

    public static DefaultValue load(TypeDescription typeDescription) {
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

    private final int opcode;
    private final Size size;

    private DefaultValue(int opcode, TypeSize typeSize) {
        this.opcode = opcode;
        this.size = typeSize.toIncreasingSize();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor) {
        if (opcode > -1) {
            methodVisitor.visitInsn(opcode);
        }
        return size;
    }
}
