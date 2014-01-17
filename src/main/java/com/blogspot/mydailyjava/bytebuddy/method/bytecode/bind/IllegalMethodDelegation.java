package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
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
    public Size apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
        throw new IllegalStateException();
    }
}
