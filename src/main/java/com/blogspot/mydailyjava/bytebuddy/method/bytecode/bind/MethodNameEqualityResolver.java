package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;

public enum MethodNameEqualityResolver implements MethodDelegationBinder.AmbiguityResolver {
    INSTANCE;

    @Override
    public Resolution resolve(MethodDescription source,
                              MethodDelegationBinder.Binding left,
                              MethodDelegationBinder.Binding right) {
        boolean leftEquals = left.getTarget().getName().equals(source.getName());
        boolean rightEquals = right.getTarget().getName().equals(source.getName());
        if (leftEquals ^ rightEquals) {
            return leftEquals ? Resolution.LEFT : Resolution.RIGHT;
        } else {
            return Resolution.AMBIGUOUS;
        }
    }
}
