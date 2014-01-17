package com.blogspot.mydailyjava.bytebuddy.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.DefaultValue;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;

public enum StubMethod implements ByteCodeAppender.Factory {
    INSTANCE;

    private static enum Appender implements ByteCodeAppender {
        INSTANCE;

        @Override
        public Size apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            return new Size(
                    DefaultValue.load(methodDescription.getReturnType()).apply(methodVisitor)
                            .aggregate(MethodReturn.returning(methodDescription.getReturnType()).apply(methodVisitor))
                            .getMaximalSize(),
                    TypeSize.sizeOf(Arrays.asList(methodDescription.getParameterTypes())) + 1);
        }
    }

    @Override
    public ByteCodeAppender make(TypeDescription typeDescription) {
        return Appender.INSTANCE;
    }
}
