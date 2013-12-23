package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;

public class PrimitiveTypeAwareAssigner implements Assigner {

    private final Assigner referenceTypeDelegate;

    public PrimitiveTypeAwareAssigner(Assigner referenceTypeDelegate) {
        this.referenceTypeDelegate = referenceTypeDelegate;
    }

    @Override
    public Assignment assign(String superTypeName, Class<?> subType, boolean considerRuntimeType) {
        boolean superTypeIsPrimitive = isPrimitive(superTypeName), subTypeIsPrimitive = subType.isPrimitive();
        if (superTypeIsPrimitive && subTypeIsPrimitive) {
            return PrimitiveWideningDelegate.forPrimitive(superTypeName).widenTo(subType);
        } else if (superTypeIsPrimitive /* && !subTypeIsPrimitive */) {
            return PrimitiveUnboxingDelegate.forPrimitive(superTypeName).boxAndAssignTo(subType, referenceTypeDelegate, considerRuntimeType);
        } else if (/* !superTypeIsPrimitive && */ subTypeIsPrimitive) {
            // TODO: Replace logic as described inside of class.
            return PrimitiveUnboxingDelegate.forType(superTypeName).unboxAndAssignTo(subType, referenceTypeDelegate, considerRuntimeType);
        } else {
            return referenceTypeDelegate.assign(superTypeName, subType, considerRuntimeType);
        }
    }

    @Override
    public Assignment assign(Class<?> superType, String subTypeName, boolean considerRuntimeType) {
        boolean superTypeIsPrimitive = superType.isPrimitive(), subTypeIsPrimitive = isPrimitive(subTypeName);
        if (superTypeIsPrimitive && subTypeIsPrimitive) {
            return PrimitiveWideningDelegate.forPrimitive(superType).widenTo(subTypeName);
        } else if (superTypeIsPrimitive /* && !subTypeIsPrimitive */) {
            return PrimitiveUnboxingDelegate.forPrimitive(superType).boxAndAssignTo(subTypeName, referenceTypeDelegate, considerRuntimeType);
        } else if (/* !superTypeIsPrimitive && */ subTypeIsPrimitive) {
            // TODO: Replace logic as described inside of class.
            return PrimitiveUnboxingDelegate.forType(superType).unboxAndAssignTo(subTypeName, referenceTypeDelegate, considerRuntimeType);
        } else {
            return referenceTypeDelegate.assign(superType, subTypeName, considerRuntimeType);
        }
    }

    private static boolean isPrimitive(String typeName) {
        return typeName.length() == 1;
    }
}
