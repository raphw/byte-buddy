package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import org.objectweb.asm.MethodVisitor;

public enum IllegalStackManipulation implements StackManipulation {
    INSTANCE;

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        throw new IllegalStateException("It is not possible to apply an illegal assignment as byte code");
    }
}
