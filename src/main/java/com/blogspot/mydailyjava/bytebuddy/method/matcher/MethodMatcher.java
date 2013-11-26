package com.blogspot.mydailyjava.bytebuddy.method.matcher;

public interface MethodMatcher {

    boolean matches(String classTypeName,
                    String methodName,
                    int methodAccess,
                    String methodDesc,
                    String methodSignature,
                    String[] methodException);
}
