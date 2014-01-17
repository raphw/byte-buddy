package com.blogspot.mydailyjava.bytebuddy.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum SuperClassDelegation implements ByteCodeAppender.Factory {
    INSTANCE;

    private static class Appender implements ByteCodeAppender {

        private final String superClassInternalName;

        private Appender(String superClassInternalName) {
            this.superClassInternalName = superClassInternalName;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            Assignment.Size size = new Assignment.Size(1, 1);
            for (Class<?> parameterType : methodDescription.getParameterTypes()) {
                size = size.aggregate(MethodArgument.loading(parameterType)
                        .fromArgumentIndex(size.getSize()).apply(methodVisitor));
            }
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    superClassInternalName,
                    methodDescription.getInternalName(),
                    methodDescription.getDescriptor());
            size = size.aggregate(MethodReturn.returning(methodDescription.getReturnType()).apply(methodVisitor));
            return new Size(size.getMaximalSize(), size.getMaximalSize());
        }
    }

    @Override
    public ByteCodeAppender make(TypeDescription typeDescription) {
        return new Appender(typeDescription.getSuperClassInternalName());
    }
}
