package net.bytebuddy.instrumentation.method.bytecode.stack;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum Removal implements StackManipulation {

    /**
     * A duplication of no values. This corresponds a no-op instruction.
     */
    ZERO(StackSize.ZERO, Opcodes.NOP) {

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            return new Size(0, 0);
        }
    },

    SINGLE(StackSize.SINGLE, Opcodes.POP),

    DOUBLE(StackSize.DOUBLE, Opcodes.POP2);

    private final Size size;

    private final int opcode;

    private Removal(StackSize stackSize, int opcode) {
        size = stackSize.toDecreasingSize();
        this.opcode = opcode;
    }

    public static StackManipulation pop(TypeDescription typeDescription) {
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

