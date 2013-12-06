package com.blogspot.mydailyjava.bytebuddy.extraction.method.stack;

import com.blogspot.mydailyjava.bytebuddy.extraction.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.context.MethodContext;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MethodArgument implements CallStackArgument {

    private final int argumentIndex;

    public MethodArgument(int argumentIndex) {
        this.argumentIndex = argumentIndex;
    }

    @Override
    public Size load(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext) {
        Type argumentType = methodContext.getType().getArgumentTypes()[argumentIndex];
        methodVisitor.visitVarInsn(argumentType.getOpcode(Opcodes.ALOAD), argumentIndex);
        // TODO: Boxing / unboxing of primitive types!
        // TODO: Size extension of primitive types!
        int argumentTypeSize = argumentType.getSize();
        return new Size(argumentTypeSize, argumentTypeSize);
    }
}
