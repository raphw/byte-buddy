package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.reference;

import org.objectweb.asm.Type;

public class ClassLoadingReferenceAssigner extends AbstractRuntimeTypeAwareAssigner {

    private static final int REFERENCE_MEMORY_SIZE = 1;
    private static final int PRIMITIVE_TYPE_NAME_SIZE = 1;

    private final ClassLoader classLoader;

    public ClassLoadingReferenceAssigner(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    protected boolean isAssignable(String superTypeName, Class<?> subType) {
        return !isPrimitive(superTypeName) && !subType.isPrimitive() && findClass(superTypeName).isAssignableFrom(subType);
    }

    @Override
    protected boolean isAssignable(Class<?> superType, String subTypeName) {
        return !superType.isPrimitive() && !isPrimitive(subTypeName) && superType.isAssignableFrom(findClass(subTypeName));
    }

    private static boolean isPrimitive(String typeName) {
        return typeName.length() == PRIMITIVE_TYPE_NAME_SIZE;
    }

    private Class<?> findClass(String internalName) {
        String className = Type.getObjectType(internalName).getClassName();
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load class: " + className, e);
        }
    }
}
