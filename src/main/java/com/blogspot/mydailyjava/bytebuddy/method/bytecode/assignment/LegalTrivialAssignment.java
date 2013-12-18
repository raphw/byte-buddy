package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment;

import com.blogspot.mydailyjava.bytebuddy.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.context.MethodContext;
import org.objectweb.asm.MethodVisitor;

public enum  LegalTrivialAssignment implements Assignment {
    INSTANCE;

    @Override
    public boolean isAssignable() {
        return true;
    }

    @Override
    public Size load(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext) {
        return new Size(0, 0);
    }
}
