package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.DefaultValue;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

public enum StubMethod implements ByteCodeAppender.Factory {
    INSTANCE;

    private static enum Appender implements ByteCodeAppender {
        INSTANCE;

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext, MethodDescription instrumentedMethod) {
            return new Size(
                    DefaultValue.load(instrumentedMethod.getReturnType()).apply(methodVisitor, instrumentationContext)
                            .aggregate(MethodReturn.returning(instrumentedMethod.getReturnType()).apply(methodVisitor, instrumentationContext))
                            .getMaximalSize(),
                    instrumentedMethod.getStackSize());
        }
    }

    @Override
    public ByteCodeAppender make(TypeDescription typeDescription) {
        return Appender.INSTANCE;
    }
}
