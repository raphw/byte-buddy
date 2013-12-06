package com.blogspot.mydailyjava.bytebuddy.extraction.method;

import com.blogspot.mydailyjava.bytebuddy.extraction.information.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.information.MethodContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.method.appender.ByteCodeAppender;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class MethodInterceptor extends ClassVisitor {

    private final AppenderSelector appenderSelector;

    private ClassContext classContext;

    public MethodInterceptor(int api, ClassVisitor cv, AppenderSelector appenderSelector) {
        super(api, cv);
        this.appenderSelector = appenderSelector;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        classContext = new ClassContext(version, access, name, signature, superName, interfaces);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        if (methodVisitor != null) {
            MethodContext methodContext = new MethodContext(access, name, desc, signature, exceptions);
            ByteCodeAppender.Size size = new ByteCodeAppender.Size(0, methodContext.getType().getArgumentsAndReturnSizes());
            methodVisitor.visitCode();
            for (ByteCodeAppender byteCodeAppender : appenderSelector.findAppenders(classContext, methodContext)) {
                size = size.merge(byteCodeAppender.apply(methodVisitor, classContext, methodContext));
            }
            methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
            methodVisitor.visitEnd();
        }
        return methodVisitor;
    }
}
