package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.reference;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.AssignmentExaminer;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.LegalTrivialAssignment;
import org.objectweb.asm.Type;

public class ClassLoadingReferenceAssignmentExaminer implements AssignmentExaminer {

    private final ClassLoader classLoader;

    public ClassLoadingReferenceAssignmentExaminer(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Assignment assign(String superTypeName, Class<?> subType) {
        if (!isPrimitive(superTypeName) && findClass(superTypeName).isAssignableFrom(subType)) {
            return LegalTrivialAssignment.INSTANCE;
        } else {
            return IllegalAssignment.INSTANCE;
        }
    }

    @Override
    public Assignment assign(Class<?> superType, String subTypeName) {
        if (!isPrimitive(subTypeName) && superType.isAssignableFrom(findClass(subTypeName))) {
            return LegalTrivialAssignment.INSTANCE;
        } else {
            return IllegalAssignment.INSTANCE;
        }
    }

    private static boolean isPrimitive(String typeName) {
        return typeName.length() == 1;
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
