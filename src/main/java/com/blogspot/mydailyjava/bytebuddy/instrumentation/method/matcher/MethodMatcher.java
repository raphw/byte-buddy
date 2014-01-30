package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;

public interface MethodMatcher {

    boolean matches(MethodDescription methodDescription);
}
