package com.blogspot.mydailyjava.bytebuddy.method.score;

import com.blogspot.mydailyjava.bytebuddy.method.stack.MethodCallStackValue;
import com.blogspot.mydailyjava.bytebuddy.type.TypeCompatibilityEvaluator;

import java.lang.reflect.Method;
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

    static class MatchedMethod implements Comparable<MatchedMethod> {

        private final double score;
        private final List<MethodCallStackValue> methodCallStackValues;

        public MatchedMethod(double score, List<MethodCallStackValue> methodCallStackValues) {
            this.score = score;
            this.methodCallStackValues = methodCallStackValues;
        }

        public double getScore() {
            return score;
        }

        public List<MethodCallStackValue> getValues() {
            return methodCallStackValues;
        }

        @Override
        public int compareTo(MatchedMethod other) {
            return (int) (score - other.score);
        }
    }

    MatchedMethod evaluate(Method method);
}
