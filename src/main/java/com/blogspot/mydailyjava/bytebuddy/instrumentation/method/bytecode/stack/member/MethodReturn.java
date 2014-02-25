package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodReturn implements StackManipulation {

    INTEGER(Opcodes.IRETURN, StackSize.SINGLE),
    DOUBLE(Opcodes.DRETURN, StackSize.DOUBLE),
    FLOAT(Opcodes.FRETURN, StackSize.SINGLE),
    LONG(Opcodes.LRETURN, StackSize.DOUBLE),
    VOID(Opcodes.RETURN, StackSize.ZERO),
    ANY_REFERENCE(Opcodes.ARETURN, StackSize.SINGLE);

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
    private final StackSize stackSize;

    private MethodReturn(int returnOpcode, StackSize stackSize) {
        this.returnOpcode = returnOpcode;
        this.stackSize = stackSize;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitInsn(returnOpcode);
        return stackSize.toDecreasingSize();
    }
}
