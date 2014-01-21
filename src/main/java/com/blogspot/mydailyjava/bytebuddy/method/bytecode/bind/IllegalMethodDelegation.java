package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.ByteCodeAppender;
import org.objectweb.asm.MethodVisitor;

public enum IllegalMethodDelegation implements MethodDelegationBinder.BoundMethodDelegation {
    INSTANCE;

    @Override
    public boolean isBound() {
        return false;
    }

    @Override
    public Integer getBindingIndex(Object identificationToken) {
        throw new IllegalStateException();
    }

    @Override
    public MethodDescription getBindingTarget() {
        throw new IllegalStateException();
    }

    @Override
    public ByteCodeAppender.Size apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
        throw new IllegalStateException();
    }
}
