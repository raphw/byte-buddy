package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;

/**
 * Implementation of an
 * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}
 * that resolves conflicting bindings by considering equality of a target method's internalName as an indicator for a dominant
 * binding.
 * <p>&nbsp;</p>
 * For example, if method {@code source.foo} can be bound to methods {@code targetA.foo} and {@code targetB.bar},
 * {@code targetA.foo} will be considered as dominant.
 */
public enum MethodNameEqualityResolver implements MethodDelegationBinder.AmbiguityResolver {

    /**
     * The singleton instance.
     */
    INSTANCE;

    @Override
    public Resolution resolve(MethodDescription source,
                              MethodDelegationBinder.MethodBinding left,
                              MethodDelegationBinder.MethodBinding right) {
        boolean leftEquals = left.getTarget().getName().equals(source.getName());
        boolean rightEquals = right.getTarget().getName().equals(source.getName());
        if (leftEquals ^ rightEquals) {
            return leftEquals ? Resolution.LEFT : Resolution.RIGHT;
        } else {
            return Resolution.AMBIGUOUS;
        }
    }
}
