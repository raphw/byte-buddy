package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
/**
 * This class is responsible for loading any {@code double} constant onto the operand stack.
 */
public enum DoubleConstant implements StackManipulation {

    ZERO(Opcodes.DCONST_0),
    ONE(Opcodes.DCONST_1);

    private static final StackManipulation.Size SIZE = StackSize.DOUBLE.toIncreasingSize();

    private static class ConstantPool implements StackManipulation {

        private final double value;

        private ConstantPool(double value) {
            this.value = value;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitLdcInsn(value);
            return SIZE;
        }
    }
    /**
     * Creates a stack manipulation for loading a {@code double} value onto the operand stack.
     * <p/>
     * This is achieved either by invoking a specific opcode, if any, or by creating a constant pool entry.
     *
     * @param value The {@code double} value to load onto the stack.
     * @return A stack manipulation for loading the given {@code double} value.
     */
    public static StackManipulation forValue(double value) {
        if (value == 0d) {
            return ZERO;
        } else if (value == 1d) {
            return ONE;
        } else {
            return new ConstantPool(value);
        }
    }

    private final int opcode;

    private DoubleConstant(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public StackManipulation.Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitInsn(opcode);
        return SIZE;
    }
}
