package com.blogspot.mydailyjava.old.type;


import java.util.Arrays;
import java.util.List;

public abstract class AbstractTypeCompatibilityEvaluator implements TypeCompatibilityEvaluator {

    @Override
    public boolean isAssignable(Class<?> superType, List<String> subTypeNames) {
        for (String subTypeName : subTypeNames) {
            if (!isAssignable(superType, subTypeName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isThrowable(Class<?>[] targetExceptionType, String[] sourceExceptionTypeName) {
        List<String> sourceExceptionTypeNames = Arrays.asList(sourceExceptionTypeName);
        for (Class<?> exceptionType : targetExceptionType) {
            if (!exceptionType.isAssignableFrom(RuntimeException.class)) {
                if (!isAssignable(exceptionType, sourceExceptionTypeNames)) {
                    return false;
                }
            }
        }
        return true;
    }
}
