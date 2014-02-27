package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation returning a value of a given type.
 */
public enum MethodReturn implements StackManipulation {

    INTEGER(Opcodes.IRETURN, StackSize.SINGLE),
    DOUBLE(Opcodes.DRETURN, StackSize.DOUBLE),
    FLOAT(Opcodes.FRETURN, StackSize.SINGLE),
    LONG(Opcodes.LRETURN, StackSize.DOUBLE),
    VOID(Opcodes.RETURN, StackSize.ZERO),
    ANY_REFERENCE(Opcodes.ARETURN, StackSize.SINGLE);

    /**
     * Returns a method return corresponding to a given type.
     *
     * @param typeDescription The type to be returned.
     * @return The stack manipulation representing the method return.
     */
    public static StackManipulation returning(TypeDescription typeDescription) {
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
    private final Size size;

    private MethodReturn(int returnOpcode, StackSize stackSize) {
        this.returnOpcode = returnOpcode;
        size = stackSize.toDecreasingSize();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitInsn(returnOpcode);
        return size;
    }
}
