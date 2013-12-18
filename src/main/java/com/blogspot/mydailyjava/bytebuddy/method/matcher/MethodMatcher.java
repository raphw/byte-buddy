package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.context.MethodContext;

public interface MethodMatcher {

    boolean matches(ClassContext classContext, MethodContext methodContext);
}
