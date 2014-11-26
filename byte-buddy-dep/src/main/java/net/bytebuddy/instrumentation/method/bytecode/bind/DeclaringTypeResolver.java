package net.bytebuddy.instrumentation.method.bytecode.bind;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;

/**
 * This ambiguity resolver matches that method out of two methods that is declared by the more specific type. If two
 * methods are declared by the same type or by two unrelated types, this resolver returns an ambiguous result.
 */
public enum DeclaringTypeResolver implements MethodDelegationBinder.AmbiguityResolver {

    /**
     * The singleton instance.
     */
    INSTANCE;

    @Override
    public Resolution resolve(MethodDescription source,
                              MethodDelegationBinder.MethodBinding left,
                              MethodDelegationBinder.MethodBinding right) {
        TypeDescription leftType = left.getTarget().getDeclaringType();
        TypeDescription rightType = right.getTarget().getDeclaringType();
        if (leftType.equals(rightType)) {
            return Resolution.AMBIGUOUS;
        } else if (leftType.isAssignableFrom(rightType)) {
            return Resolution.RIGHT;
        } else if (leftType.isAssignableTo(rightType)) {
            return Resolution.LEFT;
        } else {
            return Resolution.AMBIGUOUS;
        }
    }
}
