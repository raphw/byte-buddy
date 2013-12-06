package com.blogspot.mydailyjava.old.type;

import org.objectweb.asm.Type;

public class ClassLoadingCompatibilityEvaluator extends PlainTypeCompatibilityEvaluator {

    private final ClassLoader classLoader;

    public ClassLoadingCompatibilityEvaluator(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public boolean isAssignable(String superTypeName, Class<?> subType) {
        return super.isAssignable(superTypeName, subType) ||
                (!isPrimitive(superTypeName) && findClass(superTypeName).isAssignableFrom(subType));
    }

    @Override
    public boolean isAssignable(Class<?> superType, String subTypeName) {
        return super.isAssignable(superType, subTypeName) ||
                (!isPrimitive(subTypeName) && superType.isAssignableFrom(findClass(subTypeName)));
    }

    private boolean isPrimitive(String internalName) {
        return internalName.length() == 1;
    }

    protected Class<?> findClass(String internalName) {
        String className = Type.getObjectType(internalName).getClassName();
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String.format("Instrumentation target type %s could not be loaded", className), e);
        }
    }
}
