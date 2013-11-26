package com.blogspot.mydailyjava.bytebuddy.type;

import java.util.List;

public interface TypeCompatibilityEvaluator {

    boolean isAssignable(Class<?> superType, String subTypeName);

    boolean isAssignable(String superTypeName, Class<?> subType);

    boolean isAssignable(Class<?> superType, List<String> subTypeNames);

    boolean isThrowable(Class<?>[] targetExceptionType, String[] sourceExceptionTypeName);
}
