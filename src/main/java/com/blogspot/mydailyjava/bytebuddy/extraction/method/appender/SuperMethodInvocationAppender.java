package com.blogspot.mydailyjava.bytebuddy.extraction.method.appender;

import com.blogspot.mydailyjava.bytebuddy.extraction.information.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.information.MethodContext;
import com.blogspot.mydailyjava.old.method.MethodUtility;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SuperMethodInvocationAppender implements ByteCodeAppender {

    @Override
    public Size apply(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext) {
        int size = MethodUtility.loadThisAndArgumentsOnStack(methodVisitor, methodContext.getDescriptor());
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, classContext.getSuperName(), methodContext.getName(), methodContext.getDescriptor());
        methodVisitor.visitInsn(methodContext.getType().getReturnType().getOpcode(Opcodes.ARETURN));
        return new Size(methodContext.getType().getArgumentsAndReturnSizes(), 0);
    }
}
