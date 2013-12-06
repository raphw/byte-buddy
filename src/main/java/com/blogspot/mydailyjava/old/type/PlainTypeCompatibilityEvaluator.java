package com.blogspot.mydailyjava.old.type;

import org.objectweb.asm.Type;

public class PlainTypeCompatibilityEvaluator extends AbstractTypeCompatibilityEvaluator {

    private static final String OBJECT_NAME_INTERNAL = Type.getInternalName(Object.class);

    @Override
    public boolean isAssignable(Class<?> superType, String subTypeName) {
        String superTypeDescriptor = Type.getDescriptor(superType);
        return (superType == Object.class && isNotPrimitive(subTypeName, superType)) || superTypeDescriptor.equals(subTypeName);
    }

    @Override
    public boolean isAssignable(String superTypeName, Class<?> subType) {
        String subTypeDescriptor = Type.getDescriptor(subType);
        return (OBJECT_NAME_INTERNAL.equals(superTypeName) && isNotPrimitive(subTypeDescriptor, subType)) || superTypeName.equals(Type.getDescriptor(subType));
    }

    private boolean isNotPrimitive(String typeDescriptor, Class<?> type) {
        return typeDescriptor.length() > 1 && !type.isPrimitive();
    }
}
