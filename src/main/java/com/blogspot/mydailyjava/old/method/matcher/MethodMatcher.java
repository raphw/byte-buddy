package com.blogspot.mydailyjava.old.method.matcher;

public interface MethodMatcher {

    boolean matches(String classTypeName,
                    String methodName,
                    int methodAccess,
                    String methodDesc,
                    String methodSignature,
                    String[] methodException);
}
