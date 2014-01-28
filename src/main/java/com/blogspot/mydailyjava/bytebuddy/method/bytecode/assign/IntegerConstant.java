package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum IntegerConstant implements Assignment {

    ZERO(Opcodes.ICONST_0),
    ONE(Opcodes.ICONST_1),
    TWO(Opcodes.ICONST_2),
    THREE(Opcodes.ICONST_3),
    FOUR(Opcodes.ICONST_4),
    FIVE(Opcodes.ICONST_5);

    private static final Size SIZE = TypeSize.SINGLE.toIncreasingSize();

    private static class BiPush implements Assignment {

        private final int value;

        private BiPush(int value) {
            this.value = value;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, value);
            return SIZE;
        }
    }

    public static Assignment forValue(int value) {
        switch (value) {
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
    public Size apply(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(opcode);
        return SIZE;
    }
}
