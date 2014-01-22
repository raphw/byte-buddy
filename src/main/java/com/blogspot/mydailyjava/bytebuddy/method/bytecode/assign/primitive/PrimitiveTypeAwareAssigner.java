package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;

public class PrimitiveTypeAwareAssigner implements Assigner {

    private final Assigner nonPrimitiveAwareAssigner;

    public PrimitiveTypeAwareAssigner(Assigner nonPrimitiveAwareAssigner) {
        this.nonPrimitiveAwareAssigner = nonPrimitiveAwareAssigner;
    }

    @Override
    public Assignment assign(Class<?> superType, Class<?> subType, boolean considerRuntimeType) {
        if (superType.isPrimitive() && subType.isPrimitive()) {
            return PrimitiveWideningDelegate.forPrimitive(superType).widenTo(subType);
        } else if (superType.isPrimitive() /* && !subType.isPrimitive() */) {
            return PrimitiveUnboxingDelegate.forPrimitive(superType).boxAndAssignTo(subType, nonPrimitiveAwareAssigner, considerRuntimeType);
        } else if (/* !superType.isPrimitive() && */ subType.isPrimitive()) {
            return PrimitiveUnboxingDelegate.forNonPrimitive(superType).unboxAndAssignTo(subType, nonPrimitiveAwareAssigner, considerRuntimeType);
        } else /* !superType.isPrimitive() && !subType.isPrimitive()) */ {
            return nonPrimitiveAwareAssigner.assign(superType, subType, considerRuntimeType);
        }
    }
}
