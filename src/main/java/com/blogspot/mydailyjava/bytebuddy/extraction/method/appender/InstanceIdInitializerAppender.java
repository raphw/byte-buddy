package com.blogspot.mydailyjava.bytebuddy.extraction.method.appender;

import com.blogspot.mydailyjava.bytebuddy.extraction.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.context.MethodContext;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.UUID;

public class InstanceIdInitializerAppender extends AbstractIdInitializerAppender {

    public InstanceIdInitializerAppender(String idClassName, String idFieldName) {
        super(idClassName, idFieldName);
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext) {
        methodVisitor.visitLdcInsn(UUID.randomUUID().toString());
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, UUID_TYPE_NAME, UUID_METHOD_NAME, UUID_METHOD_DESCRIPTION);
        methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, getTargetClassName(), getTargetFieldName(), UUID_TYPE_NAME_INTERNAL_FORM);
        return new Size(1, 0);
    }
}
