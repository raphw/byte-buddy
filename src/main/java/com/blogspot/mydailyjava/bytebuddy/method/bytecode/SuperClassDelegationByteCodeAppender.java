package com.blogspot.mydailyjava.bytebuddy.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodReturn;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

public class SuperClassDelegationByteCodeAppender implements ByteCodeAppender {

    private static final String CONSTRUCTOR_METHOD_NAME = "<init>";

    private final String superClass;

    public SuperClassDelegationByteCodeAppender(Class<?> type) {
        this.superClass = Type.getInternalName(type);
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Method method) {
        return apply(methodVisitor, method.getParameterTypes(), method.getReturnType(), method.getName(), Type.getMethodDescriptor(method));
    }

    public Size apply(MethodVisitor methodVisitor, Constructor<?> constructor) {
        return apply(methodVisitor, constructor.getParameterTypes(), void.class, CONSTRUCTOR_METHOD_NAME, Type.getConstructorDescriptor(constructor));
    }

    private Size apply(MethodVisitor methodVisitor, Class<?>[] parameterTypes, Class<?> returnType, String methodName, String methodDescriptor) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        Assignment.Size size = new Assignment.Size(1, 1);
        for (Class<?> parameterType : parameterTypes) {
            size = size.aggregateLeftFirst(MethodArgument.loading(parameterType).apply(size.getSize(), methodVisitor));
        }
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, superClass, methodName, methodDescriptor);
        size = size.aggregateLeftFirst(MethodReturn.returning(returnType).apply(methodVisitor));
        return new Size(size.getMaximalSize(), ValueSize.sizeOf(Arrays.asList(parameterTypes)) + 1);
    }
}
