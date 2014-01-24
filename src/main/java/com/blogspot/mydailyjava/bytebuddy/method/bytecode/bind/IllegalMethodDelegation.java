package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import org.objectweb.asm.MethodVisitor;

public enum IllegalMethodDelegation implements MethodDelegationBinder.Binding {
    INSTANCE;

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public Integer getTargetParameterIndex(Object identificationToken) {
        throw new IllegalStateException();
    }

    @Override
    public MethodDescription getTarget() {
        throw new IllegalStateException();
    }

    @Override
    public Assignment.Size apply(MethodVisitor methodVisitor) {
        throw new IllegalStateException();
    }
}
