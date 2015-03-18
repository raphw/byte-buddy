package net.bytebuddy.instrumentation.method.bytecode.bind;

import net.bytebuddy.instrumentation.method.MethodDescription;

/**
 * This {@link net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver} selects
 * the method with more arguments. If two methods have equally many arguments, the resolution is ambiguous.
 */
public enum ParameterLengthResolver implements MethodDelegationBinder.AmbiguityResolver {

    /**
     * The singleton instance.
     */
    INSTANCE;

    @Override
    public Resolution resolve(MethodDescription source,
                              MethodDelegationBinder.MethodBinding left,
                              MethodDelegationBinder.MethodBinding right) {
        int leftLength = left.getTarget().getParameters().size();
        int rightLength = right.getTarget().getParameters().size();
        if (leftLength == rightLength) {
            return Resolution.AMBIGUOUS;
        } else if (leftLength < rightLength) {
            return Resolution.RIGHT;
        } else {
            return Resolution.LEFT;
        }
    }
}
