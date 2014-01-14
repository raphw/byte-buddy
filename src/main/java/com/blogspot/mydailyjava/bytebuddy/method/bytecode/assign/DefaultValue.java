package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum DefaultValue implements Assignment {

    INTEGER(Opcodes.ICONST_0, new Size(0, 1)),
    LONG(Opcodes.LCONST_0, new Size(0, 2)),
    FLOAT(Opcodes.FCONST_0, new Size(0, 1)),
    DOUBLE(Opcodes.DCONST_0, new Size(0, 2)),
    VOID(-1, new Size(0, 0)),
    ANY_REFERENCE(Opcodes.ACONST_NULL, new Size(0, 1));

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

    private DefaultValue(int opcode, Size size) {
        this.opcode = opcode;
        this.size = size;
    }

    @Override
    public boolean isAssignable() {
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
