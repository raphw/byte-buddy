package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment;

public interface AssignmentExaminer {

    Assignment assign(String superTypeName, Class<?> subType);

    Assignment assign(Class<?> superType, String subTypeName);
}
