package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import org.objectweb.asm.MethodVisitor;

public class LegalTrivialAssignment implements Assignment {

    private final Size size;

    public LegalTrivialAssignment(int size) {
        this.size = new Size(size, size);
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
