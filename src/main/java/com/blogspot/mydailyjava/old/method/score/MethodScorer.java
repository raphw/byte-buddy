package com.blogspot.mydailyjava.old.method.score;

import com.blogspot.mydailyjava.old.method.stack.MethodCallStackValue;
import com.blogspot.mydailyjava.old.type.TypeCompatibilityEvaluator;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public interface MethodScorer {

    static interface Factory {

        MethodScorer make(String classTypeName,
                          String methodName,
                          int methodAccess,
                          String methodDesc,
                          String methodSignature,
                          String[] methodException,
                          TypeCompatibilityEvaluator typeCompatibilityEvaluator);
    }

    static class NoMatchException extends Exception {
        // Used to signal a non-matching method.
    }

    static class MatchedMethod {

        private final List<MethodCallStackValue> methodCallStackValues;

        public MatchedMethod(List<MethodCallStackValue> methodCallStackValues) {
            this.methodCallStackValues = Collections.unmodifiableList(methodCallStackValues);
        }

        public List<MethodCallStackValue> getValues() {
            return methodCallStackValues;
        }
    }

    MatchedMethod match(Method method) throws NoMatchException;
}
