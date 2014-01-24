package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum DefaultValue implements Assignment {

    INTEGER(Opcodes.ICONST_0, TypeSize.SINGLE),
    LONG(Opcodes.LCONST_0, TypeSize.DOUBLE),
    FLOAT(Opcodes.FCONST_0, TypeSize.SINGLE),
    DOUBLE(Opcodes.DCONST_0, TypeSize.DOUBLE),
    VOID(-1, TypeSize.NONE),
    ANY_REFERENCE(Opcodes.ACONST_NULL, TypeSize.SINGLE);

    public static DefaultValue load(Class<?> type) {
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
