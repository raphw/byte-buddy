package com.blogspot.mydailyjava.bytebuddy.extraction.method;

import com.blogspot.mydailyjava.bytebuddy.extraction.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.context.MethodContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.method.appender.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.interception.method.matching.MethodMatcher;

import java.util.Collections;
import java.util.List;

public class AppenderSelector {

    public static final class Entry {

        private final MethodMatcher methodMatcher;
        private final List<? extends ByteCodeAppender> methodCodeAppenders;

        public Entry(MethodMatcher methodMatcher, List<? extends ByteCodeAppender> methodCodeAppenders) {
            this.methodMatcher = methodMatcher;
            this.methodCodeAppenders = methodCodeAppenders;
        }

        public MethodMatcher getMethodMatcher() {
            return methodMatcher;
        }

        public List<? extends ByteCodeAppender> getMethodCodeAppenders() {
            return methodCodeAppenders;
        }
    }

    private final List<Entry> entries;

    public AppenderSelector(List<Entry> entries) {
        this.entries = entries;
    }

    public List<? extends ByteCodeAppender> findAppenders(ClassContext classContext, MethodContext methodContext) {
        for (Entry entry : entries) {
            if (entry.getMethodMatcher().matches(classContext, methodContext)) {
                return entry.getMethodCodeAppenders();
            }
        }
        return Collections.emptyList();
    }
}
