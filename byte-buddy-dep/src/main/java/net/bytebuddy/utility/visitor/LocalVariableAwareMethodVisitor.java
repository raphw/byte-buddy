package net.bytebuddy.utility.visitor;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class LocalVariableAwareMethodVisitor extends MethodVisitor {

    private int freeOffset;

    protected LocalVariableAwareMethodVisitor(MethodVisitor methodVisitor, MethodDescription methodDescription) {
        super(OpenedClassReader.ASM_API, methodVisitor);
        freeOffset = methodDescription.getStackSize();
    }

    @Override
    public void visitVarInsn(int opcode, int offset) {
        switch (opcode) {
            case Opcodes.ISTORE:
            case Opcodes.FSTORE:
            case Opcodes.ASTORE:
                freeOffset = Math.max(freeOffset, offset + 1);
                break;
            case Opcodes.LSTORE:
            case Opcodes.DSTORE:
                freeOffset = Math.max(freeOffset, offset + 2);
                break;
        }
        super.visitVarInsn(opcode, offset);
    }

    protected int getFreeOffset() {
        return freeOffset;
    }
}
