package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import org.objectweb.asm.MethodVisitor;

public enum LegalTrivialAssignment implements Assignment {
    INSTANCE(new Size(0, 0));

    private final Size size;

    LegalTrivialAssignment(Size size) {
        this.size = size;
    }

    @Override
    public boolean isAssignable() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor) {
        return size;
    }
}
