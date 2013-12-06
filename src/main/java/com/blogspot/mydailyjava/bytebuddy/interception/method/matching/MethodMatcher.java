package com.blogspot.mydailyjava.bytebuddy.interception.method.matching;

import com.blogspot.mydailyjava.bytebuddy.extraction.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.context.MethodContext;

public interface MethodMatcher {

    boolean matches(ClassContext classContext, MethodContext methodContext);
}
