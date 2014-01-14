package com.blogspot.mydailyjava.bytebuddy.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.method.JavaMethod;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.DefaultValue;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodReturn;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;

public enum StubMethod implements ByteCodeAppender {
    INSTANCE;

    @Override
    public Size apply(MethodVisitor methodVisitor, JavaMethod javaMethod) {
        return new Size(
                MethodReturn.returning(javaMethod.getReturnType())
                        .returnAfter(DefaultValue.load(javaMethod.getReturnType()))
                        .apply(methodVisitor).getMaximalSize(),
                TypeSize.sizeOf(Arrays.asList(javaMethod.getParameterTypes())) + 1);
    }
}
