package com.blogspot.mydailyjava.bytebuddy.interception.method.matching;

import com.blogspot.mydailyjava.bytebuddy.extraction.information.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.information.MethodContext;

public interface MethodMatcher {

    boolean matches(ClassContext classContext, MethodContext methodContext);
}
