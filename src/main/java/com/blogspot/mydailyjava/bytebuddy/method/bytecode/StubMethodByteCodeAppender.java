package com.blogspot.mydailyjava.bytebuddy.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.DefaultValue;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodReturn;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;
import java.util.Arrays;

public enum StubMethodByteCodeAppender implements ByteCodeAppender {
    INSTANCE;

    @Override
    public Size apply(MethodVisitor methodVisitor, Method method) {
        return new Size(
                MethodReturn.returning(method.getReturnType())
                        .returnAfter(DefaultValue.defaulting(method.getReturnType())).apply(methodVisitor).getMaximalSize(),
                ValueSize.sizeOf(Arrays.asList(method.getParameterTypes())) + 1);
    }
}
