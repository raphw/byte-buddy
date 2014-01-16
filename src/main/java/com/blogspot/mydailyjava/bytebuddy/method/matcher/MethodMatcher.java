package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;

public interface MethodMatcher {

    boolean matches(MethodDescription methodDescription);
}
