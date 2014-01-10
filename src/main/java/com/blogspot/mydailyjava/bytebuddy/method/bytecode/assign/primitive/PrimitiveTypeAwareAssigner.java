package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;

public class PrimitiveTypeAwareAssigner implements Assigner {

    private final Assigner chainedDelegate;

    public PrimitiveTypeAwareAssigner(Assigner chainedDelegate) {
        this.chainedDelegate = chainedDelegate;
    }

    @Override
    public Assignment assign(Class<?> superType, Class subType, boolean considerRuntimeType) {
        if (superType.isPrimitive() && subType.isPrimitive()) {
            return PrimitiveWideningDelegate.forPrimitive(superType).widenTo(subType);
        } else if (superType.isPrimitive() /* && !subType.isPrimitive() */) {
            return PrimitiveUnboxingDelegate.forPrimitive(superType).boxAndAssignTo(subType, chainedDelegate, considerRuntimeType);
        } else if (/* !superType.isPrimitive() && */ subType.isPrimitive()) {
            return PrimitiveUnboxingDelegate.forNonPrimitive(superType).unboxAndAssignTo(subType, chainedDelegate, considerRuntimeType);
        } else /* !superType.isPrimitive() && !subType.isPrimitive()) */ {
            return chainedDelegate.assign(superType, subType, considerRuntimeType);
        }
    }
}
