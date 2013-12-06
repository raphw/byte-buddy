package com.blogspot.mydailyjava.bytebuddy.extraction.method.appender;

import com.blogspot.mydailyjava.bytebuddy.extraction.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.context.MethodContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.method.stack.CallStackArgument;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class StaticMethodInvocationAppender implements ByteCodeAppender {

    private final ClassContext targetClassContext;
    private final MethodContext targetMethodContext;
    private final List<? extends CallStackArgument> callStackArguments;

    public StaticMethodInvocationAppender(ClassContext targetClassContext, MethodContext targetMethodContext,
                                          List<? extends CallStackArgument> callStackArguments) {
        this.targetClassContext = targetClassContext;
        this.targetMethodContext = targetMethodContext;
        this.callStackArguments = callStackArguments;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext) {
        CallStackArgument.Size size = new CallStackArgument.Size(0, 0);
        for (CallStackArgument callStackArgument : callStackArguments) {
            size.merge(callStackArgument.load(methodVisitor, classContext, methodContext));
        }
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, targetClassContext.getName(), targetMethodContext.getName(), targetMethodContext.getDescriptor());
        methodVisitor.visitInsn(targetMethodContext.getType().getReturnType().getOpcode(Opcodes.ARETURN));
        return new Size(size.getMaximalSize(), 0);
    }
}
