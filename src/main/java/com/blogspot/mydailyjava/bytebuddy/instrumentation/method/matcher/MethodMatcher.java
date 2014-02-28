package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;

/**
 * A method matcher that allows to identify {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription}s.
 */
public interface MethodMatcher {

    /**
     * Determines if a method matches for this {@code MethodMatcher}.
     *
     * @param methodDescription The method description to be matched.
     * @return {@code true} if the matcher is matching this method.
     */
    boolean matches(MethodDescription methodDescription);
}
