package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.AssignmentExaminer;

public class PrimitiveTypeAwareAssignmentExaminer implements AssignmentExaminer {

    private final AssignmentExaminer referenceTypeDelegate;

    public PrimitiveTypeAwareAssignmentExaminer(AssignmentExaminer referenceTypeDelegate) {
        this.referenceTypeDelegate = referenceTypeDelegate;
    }

    @Override
    public Assignment assign(String superTypeName, Class<?> subType) {
        boolean superTypeIsPrimitive = isPrimitive(superTypeName), subTypeIsPrimitive = subType.isPrimitive();
        if (superTypeIsPrimitive && subTypeIsPrimitive) {
            return PrimitiveWideningAssigner.of(superTypeName).widenTo(subType);
        } else if (superTypeIsPrimitive /* && !subTypeIsPrimitive */) {
            return PrimitiveTypeBoxer.of(superTypeName).boxingAssignmentBefore(referenceTypeDelegate, subType);
        } else if (/* !superTypeIsPrimitive && */ subTypeIsPrimitive) {
            return PrimitiveTypeBoxer.of(superTypeName).unboxingAssignmentTo(subType);
        } else {
            return referenceTypeDelegate.assign(superTypeName, subType);
        }
    }

    @Override
    public Assignment assign(Class<?> superType, String subTypeName) {
        boolean superTypeIsPrimitive = superType.isPrimitive(), subTypeIsPrimitive = isPrimitive(subTypeName);
        if (superTypeIsPrimitive && subTypeIsPrimitive) {
            return PrimitiveWideningAssigner.of(superType).widenTo(subTypeName);
        } else if (superTypeIsPrimitive /* && !subTypeIsPrimitive */) {
            return PrimitiveTypeBoxer.of(superType).boxingAssignmentBefore(referenceTypeDelegate, subTypeName);
        } else if (/* !superTypeIsPrimitive && */ subTypeIsPrimitive) {
            return PrimitiveTypeBoxer.of(superType).unboxingAssignmentTo(subTypeName);
        } else {
            return referenceTypeDelegate.assign(superType, subTypeName);
        }
    }

    private static boolean isPrimitive(String typeName) {
        return typeName.length() == 1;
    }
}
