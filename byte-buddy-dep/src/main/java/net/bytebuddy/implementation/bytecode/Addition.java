package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum Addition implements StackManipulation {
    
    INTEGER(Opcodes.IADD, StackSize.SINGLE),
    
    LONG(Opcodes.LADD, StackSize.DOUBLE),
    
    FLOAT(Opcodes.FADD, StackSize.SINGLE),

    DOUBLE(Opcodes.DADD, StackSize.DOUBLE);

    private final int opcode;

    private final StackSize stackSize;

    Addition(int opcode, StackSize stackSize) {
        this.opcode = opcode;
        this.stackSize = stackSize;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitInsn(opcode);
        return stackSize.toDecreasingSize();
    }
}
