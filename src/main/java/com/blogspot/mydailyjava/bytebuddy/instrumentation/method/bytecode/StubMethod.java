package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.DefaultValue;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

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
                    methodDescription.getStackSize());
        }
    }

    @Override
    public ByteCodeAppender make(TypeDescription typeDescription) {
        return Appender.INSTANCE;
    }
}
