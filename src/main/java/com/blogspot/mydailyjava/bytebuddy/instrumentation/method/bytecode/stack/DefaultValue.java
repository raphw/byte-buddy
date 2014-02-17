package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum DefaultValue implements StackManipulation {

    INTEGER(Opcodes.ICONST_0, StackSize.SINGLE),
    LONG(Opcodes.LCONST_0, StackSize.DOUBLE),
    FLOAT(Opcodes.FCONST_0, StackSize.SINGLE),
    DOUBLE(Opcodes.DCONST_0, StackSize.DOUBLE),
    VOID(-1, StackSize.ZERO),
    ANY_REFERENCE(Opcodes.ACONST_NULL, StackSize.SINGLE);

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

    private DefaultValue(int opcode, StackSize stackSize) {
        this.opcode = opcode;
        this.size = stackSize.toIncreasingSize();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        if (opcode > -1) {
            methodVisitor.visitInsn(opcode);
        }
        return size;
    }
}
