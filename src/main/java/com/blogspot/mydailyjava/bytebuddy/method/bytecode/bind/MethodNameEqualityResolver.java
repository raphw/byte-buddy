package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;

public enum MethodNameEqualityResolver implements MethodDelegationBinder.AmbiguityResolver {
    INSTANCE;

    @Override
    public Resolution resolve(MethodDescription source,
                              MethodDelegationBinder.BoundMethodDelegation left,
                              MethodDelegationBinder.BoundMethodDelegation right) {
        boolean leftEquals = left.getBindingTarget().getName().equals(source.getName());
        boolean rightEquals = right.getBindingTarget().getName().equals(source.getName());
        if (leftEquals ^ rightEquals) {
            return leftEquals ? Resolution.LEFT : Resolution.RIGHT;
        } else {
            return Resolution.AMBIGUOUS;
        }
    }
}
