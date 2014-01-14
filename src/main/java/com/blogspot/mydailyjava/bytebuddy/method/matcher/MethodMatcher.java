package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.method.JavaMethod;

public interface MethodMatcher {

    boolean matches(JavaMethod javaMethod);
}
