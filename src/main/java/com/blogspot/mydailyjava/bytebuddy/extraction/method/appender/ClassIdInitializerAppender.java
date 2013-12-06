package com.blogspot.mydailyjava.bytebuddy.extraction.method.appender;

import com.blogspot.mydailyjava.bytebuddy.extraction.information.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.information.MethodContext;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.UUID;

public class ClassIdInitializerAppender extends AbstractIdInitializerAppender {

    public ClassIdInitializerAppender(String idClassName, String idFieldName) {
        super(idClassName, idFieldName);
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitLdcInsn(UUID.randomUUID().toString());
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, UUID_TYPE_NAME, UUID_METHOD_NAME, UUID_METHOD_DESCRIPTION);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, getTargetClassName(), getTargetFieldName(), UUID_TYPE_NAME_INTERNAL_FORM);
        return new Size(2, 0);
    }
}
