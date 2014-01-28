package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;

public class PrimitiveTypeAwareAssigner implements Assigner {

    private final Assigner referenceTypeAwareAssigner;

    public PrimitiveTypeAwareAssigner(Assigner referenceTypeAwareAssigner) {
        this.referenceTypeAwareAssigner = referenceTypeAwareAssigner;
    }

    @Override
    public Assignment assign(Class<?> sourceType, Class<?> targetType, boolean considerRuntimeType) {
        if (sourceType.isPrimitive() && targetType.isPrimitive()) {
            return PrimitiveWideningDelegate.forPrimitive(sourceType).widenTo(targetType);
        } else if (sourceType.isPrimitive() /* && !subType.isPrimitive() */) {
            return PrimitiveBoxingDelegate.forPrimitive(sourceType).assignBoxedTo(targetType, referenceTypeAwareAssigner, considerRuntimeType);
        } else if (/* !superType.isPrimitive() && */ targetType.isPrimitive()) {
            return PrimitiveUnboxingDelegate.forReferenceType(sourceType).assignUnboxedTo(targetType, referenceTypeAwareAssigner, considerRuntimeType);
        } else /* !superType.isPrimitive() && !subType.isPrimitive()) */ {
            return referenceTypeAwareAssigner.assign(sourceType, targetType, considerRuntimeType);
        }
    }
}
