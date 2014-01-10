package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import java.lang.reflect.Method;

public interface MethodMatcher {

    boolean matches(Method method);
}
