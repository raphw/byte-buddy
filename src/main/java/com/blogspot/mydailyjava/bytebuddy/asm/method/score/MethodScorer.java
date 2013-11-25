package com.blogspot.mydailyjava.bytebuddy.asm.method.score;

import com.blogspot.mydailyjava.bytebuddy.asm.method.stack.MethodCallStackValue;

import java.lang.reflect.Method;
import java.util.List;

public interface MethodScorer {

    static interface Factory {

        MethodScorer make(String classTypeName, int access, String desc, String signature, String[] exceptions);
    }

    static class ScoredMethodDelegation implements Comparable<ScoredMethodDelegation> {

        private final int score;
        private final List<MethodCallStackValue> methodCallStackValues;

        public ScoredMethodDelegation(int score, List<MethodCallStackValue> methodCallStackValues) {
            this.score = score;
            this.methodCallStackValues = methodCallStackValues;
        }

        public int getScore() {
            return score;
        }

        public List<MethodCallStackValue> getValues() {
            return methodCallStackValues;
        }

        @Override
        public int compareTo(ScoredMethodDelegation other) {
            return score - other.score;
        }
    }

    ScoredMethodDelegation evaluate(Method method);
}
