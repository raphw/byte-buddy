package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


/**
 * This class is responsible for loading any {@code int} constant onto the operand stack. Note that within the JVM,
 * {@code boolean}, {@code byte}, {@code short} and {@code char} values are represented by integers and can therefore
 * be loaded by using this class.
 */
public enum IntegerConstant implements StackManipulation {

    MINUS_ONE(Opcodes.ICONST_M1),
    ZERO(Opcodes.ICONST_0),
    ONE(Opcodes.ICONST_1),
    TWO(Opcodes.ICONST_2),
    THREE(Opcodes.ICONST_3),
    FOUR(Opcodes.ICONST_4),
    FIVE(Opcodes.ICONST_5);

    private static final Size SIZE = StackSize.SINGLE.toIncreasingSize();

    private static class BiPush implements StackManipulation {

        private final int value;

        private BiPush(int value) {
            this.value = value;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, value);
            return SIZE;
        }
    }

    /**
     * Creates a stack manipulation for loading a boolean value onto the stack.
     *
     * @param value The {@code boolean} to load onto the stack.
     * @return The stack manipulation for loading this {@code boolean}.
     */
    public static StackManipulation forValue(boolean value) {
        return value ? ONE : ZERO;
    }

    /**
     * Creates a stack manipulation for loading an {@code int} value onto the stack.
     * <p/>
     * This is achieved either by invoking a constant opcode, if any, or by creating a binary push operation.
     *
     * @param value The {@code int} (or {@code byte}, {@code short}, {@code char}) value to load onto the stack.
     * @return A stack manipulation for loading the given value.
     */
    public static StackManipulation forValue(int value) {
        switch (value) {
            case -1:
                return MINUS_ONE;
            case 0:
                return ZERO;
            case 1:
                return ONE;
            case 2:
                return TWO;
            case 3:
                return THREE;
            case 4:
                return FOUR;
            case 5:
                return FIVE;
            default:
                return new BiPush(value);
        }
    }

    private final int opcode;

    private IntegerConstant(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitInsn(opcode);
        return SIZE;
    }
}
