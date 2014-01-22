package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;

public class PrimitiveTypeAwareAssigner implements Assigner {

    private final Assigner referenceTypeAwareAssigner;

    public PrimitiveTypeAwareAssigner(Assigner referenceTypeAwareAssigner) {
        this.referenceTypeAwareAssigner = referenceTypeAwareAssigner;
    }

    @Override
    public Assignment assign(Class<?> superType, Class<?> subType, boolean considerRuntimeType) {
        if (superType.isPrimitive() && subType.isPrimitive()) {
            return PrimitiveWideningDelegate.forPrimitive(superType).widenTo(subType);
        } else if (superType.isPrimitive() /* && !subType.isPrimitive() */) {
            return PrimitiveBoxingDelegate.forPrimitive(superType).assignBoxedTo(subType, referenceTypeAwareAssigner, considerRuntimeType);
        } else if (/* !superType.isPrimitive() && */ subType.isPrimitive()) {
            return PrimitiveUnboxingDelegate.forReferenceType(superType).assignUnboxedTo(subType, referenceTypeAwareAssigner, considerRuntimeType);
        } else /* !superType.isPrimitive() && !subType.isPrimitive()) */ {
            return referenceTypeAwareAssigner.assign(superType, subType, considerRuntimeType);
        }
    }
}
