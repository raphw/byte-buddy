package com.blogspot.mydailyjava.bytebuddy.extraction.method.stack;

import com.blogspot.mydailyjava.bytebuddy.extraction.information.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.information.MethodContext;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class AllMethodArgumentsAsArray implements CallStackArgument {

    private final String arrayTypeName;

    public AllMethodArgumentsAsArray() {
        this.arrayTypeName = "java/lang/Object";
    }

    @Override
    public Size load(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext) {
        methodVisitor.visitIntInsn(Opcodes.BIPUSH, methodContext.getType().getArgumentTypes().length);
        methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, arrayTypeName);
        int currentMaximumOperandStackSize = 1;
        int currentLocalVariableIndex = 1, currentArrayIndex = 0;
        for (Type argumentType : methodContext.getType().getArgumentTypes()) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, currentArrayIndex++);
            methodVisitor.visitIntInsn(argumentType.getOpcode(Opcodes.ALOAD), currentLocalVariableIndex);
            // TODO: Boxing of primitive types!
            methodVisitor.visitInsn(Opcodes.AASTORE);
            currentLocalVariableIndex += argumentType.getSize();
            currentMaximumOperandStackSize = Math.max(currentMaximumOperandStackSize, argumentType.getSize() + 3);
        }
        return new Size(1, currentMaximumOperandStackSize);
    }
}
