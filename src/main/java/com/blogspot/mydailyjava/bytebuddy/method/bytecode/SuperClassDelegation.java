package com.blogspot.mydailyjava.bytebuddy.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.method.JavaMethod;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodReturn;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

public enum  SuperClassDelegation implements ByteCodeAppender {
    INSTANCE;

    @Override
    public Size apply(MethodVisitor methodVisitor, JavaMethod javaMethod) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        Assignment.Size size = new Assignment.Size(1, 1);
        for (Class<?> parameterType : javaMethod.getParameterTypes()) {
            size = size.aggregateLeftFirst(MethodArgument.loading(parameterType).apply(size.getSize(), methodVisitor));
        }
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, javaMethod.getDeclaringSuperClassInternalName(), javaMethod.getInternalName(), javaMethod.getDescriptor());
        size = size.aggregateLeftFirst(MethodReturn.returning(javaMethod.getReturnType()).apply(methodVisitor));
        return new Size(size.getMaximalSize(), TypeSize.sizeOf(Arrays.asList(javaMethod.getParameterTypes())) + 1);
    }
}
