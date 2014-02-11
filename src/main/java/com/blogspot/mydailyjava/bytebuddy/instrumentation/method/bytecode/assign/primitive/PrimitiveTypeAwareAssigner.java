package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

public class PrimitiveTypeAwareAssigner implements Assigner {

    private final Assigner referenceTypeAwareAssigner;

    public PrimitiveTypeAwareAssigner(Assigner referenceTypeAwareAssigner) {
        this.referenceTypeAwareAssigner = referenceTypeAwareAssigner;
    }

    @Override
    public Assignment assign(TypeDescription sourceType, TypeDescription targetType, boolean considerRuntimeType) {
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

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && referenceTypeAwareAssigner.equals(((PrimitiveTypeAwareAssigner) other).referenceTypeAwareAssigner);
    }

    @Override
    public int hashCode() {
        return referenceTypeAwareAssigner.hashCode();
    }

    @Override
    public String toString() {
        return "PrimitiveTypeAwareAssigner{chained=" + referenceTypeAwareAssigner + '}';
    }
}
