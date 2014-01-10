package com.blogspot.mydailyjava.bytebuddy.method;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodMatcher;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Interception {

    public static class Stack {

        private final List<Interception> interceptions;

        public Stack() {
            this.interceptions = Collections.emptyList();
        }

        protected Stack(List<Interception> interceptions) {
            this.interceptions = interceptions;
        }

        public ByteCodeAppender findInterceptorFor(Method method) {
            for (Interception interception : interceptions) {
                if (interception.getMethodMatcher().matches(method)) {
                    return interception.getByteCodeAppender();
                }
            }
            return null;
        }

        public Stack append(Interception interception) {
            List<Interception> interceptions = new ArrayList<Interception>(this.interceptions.size() + 1);
            interceptions.addAll(this.interceptions);
            interceptions.add(interception);
            return new Stack(Collections.unmodifiableList(interceptions));
        }
    }

    private final MethodMatcher methodMatcher;
    private final ByteCodeAppender byteCodeAppender;

    public Interception(MethodMatcher methodMatcher, ByteCodeAppender byteCodeAppender) {
        this.methodMatcher = methodMatcher;
        this.byteCodeAppender = byteCodeAppender;
    }

    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    public ByteCodeAppender getByteCodeAppender() {
        return byteCodeAppender;
    }
}
