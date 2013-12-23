package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.AssignmentExaminer;

public class PrimitiveTypeAwareAssignmentExaminer implements AssignmentExaminer {

    private final AssignmentExaminer referenceTypeDelegate;

    public PrimitiveTypeAwareAssignmentExaminer(AssignmentExaminer referenceTypeDelegate) {
        this.referenceTypeDelegate = referenceTypeDelegate;
    }

    @Override
    public Assignment assign(String superTypeName, Class<?> subType, boolean considerRuntimeType) {
        boolean superTypeIsPrimitive = isPrimitive(superTypeName), subTypeIsPrimitive = subType.isPrimitive();
        if (superTypeIsPrimitive && subTypeIsPrimitive) {
            return PrimitiveWideningAssigner.of(superTypeName).widenTo(subType);
        } else if (superTypeIsPrimitive /* && !subTypeIsPrimitive */) {
            return PrimitiveTypeBoxer.of(superTypeName).boxAndAssignTo(subType, referenceTypeDelegate, considerRuntimeType);
        } else if (/* !superTypeIsPrimitive && */ subTypeIsPrimitive) {
            // TODO: Check super type for unboxing abilities instead of sub type (only relevant for widening).
            return PrimitiveTypeBoxer.of(subType).unboxAndAssignTo(subType, referenceTypeDelegate);
        } else {
            return referenceTypeDelegate.assign(superTypeName, subType, considerRuntimeType);
        }
    }

    @Override
    public Assignment assign(Class<?> superType, String subTypeName, boolean considerRuntimeType) {
        boolean superTypeIsPrimitive = superType.isPrimitive(), subTypeIsPrimitive = isPrimitive(subTypeName);
        if (superTypeIsPrimitive && subTypeIsPrimitive) {
            return PrimitiveWideningAssigner.of(superType).widenTo(subTypeName);
        } else if (superTypeIsPrimitive /* && !subTypeIsPrimitive */) {
            return PrimitiveTypeBoxer.of(superType).boxAndAssignTo(subTypeName, referenceTypeDelegate, considerRuntimeType);
        } else if (/* !superTypeIsPrimitive && */ subTypeIsPrimitive) {
            // TODO: Check super type for unboxing abilities instead of sub type (only relevant for widening).
            return PrimitiveTypeBoxer.of(subTypeName).unboxAndAssignTo(subTypeName, referenceTypeDelegate);
        } else {
            return referenceTypeDelegate.assign(superType, subTypeName, considerRuntimeType);
        }
    }

    private static boolean isPrimitive(String typeName) {
        return typeName.length() == 1;
    }
}
