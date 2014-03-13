package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum Duplication implements StackManipulation {
    ZERO(StackSize.ZERO, Opcodes.NOP),
    SINGLE(StackSize.SINGLE, Opcodes.DUP),
    DOUBLE(StackSize.DOUBLE, Opcodes.DUP2);

    public static StackManipulation duplicate(TypeDescription typeDescription) {
        switch (typeDescription.getStackSize()) {
            case SINGLE:
                return SINGLE;
            case DOUBLE:
                return DOUBLE;
            case ZERO:
                return ZERO;
            default:
                throw new AssertionError();
        }
    }

    private final Size size;
    private final int opcode;

    Duplication(StackSize stackSize, int opcode) {
        size = stackSize.toIncreasingSize();
        this.opcode = opcode;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitInsn(opcode);
        return size;
    }
}
