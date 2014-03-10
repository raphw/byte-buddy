package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;

public enum ParameterLengthResolver implements MethodDelegationBinder.AmbiguityResolver {
    INSTANCE;

    @Override
    public Resolution resolve(MethodDescription source,
                              MethodDelegationBinder.MethodBinding left,
                              MethodDelegationBinder.MethodBinding right) {
        int leftLength = left.getTarget().getParameterTypes().size();
        int rightLength = right.getTarget().getParameterTypes().size();
        if (leftLength == rightLength) {
            return Resolution.AMBIGUOUS;
        } else if (leftLength < rightLength) {
            return Resolution.RIGHT;
        } else {
            return Resolution.LEFT;
        }
    }
}
