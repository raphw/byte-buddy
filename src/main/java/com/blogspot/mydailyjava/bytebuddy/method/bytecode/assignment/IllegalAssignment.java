package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment;

import com.blogspot.mydailyjava.bytebuddy.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.context.MethodContext;
import org.objectweb.asm.MethodVisitor;

public enum IllegalAssignment implements Assignment {
    INSTANCE;

    @Override
    public boolean isAssignable() {
        return false;
    }

    @Override
    public Size load(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext) {
        throw new IllegalStateException("It is not possible to apply an illegal assignment as byte code");
    }
}
