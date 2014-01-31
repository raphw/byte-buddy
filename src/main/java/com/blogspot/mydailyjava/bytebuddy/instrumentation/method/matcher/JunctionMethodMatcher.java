package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher;


import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;

public interface JunctionMethodMatcher extends MethodMatcher {

    static abstract class AbstractBase implements JunctionMethodMatcher {

        @Override
        public JunctionMethodMatcher and(MethodMatcher other) {
            return new Conjunction(this, other);
        }

        @Override
        public JunctionMethodMatcher or(MethodMatcher other) {
            return new Disjunction(this, other);
        }
    }

    static class Conjunction extends AbstractBase {

        private final MethodMatcher left, right;

        public Conjunction(MethodMatcher left, MethodMatcher right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return left.matches(methodDescription) && right.matches(methodDescription);
        }
    }

    static class Disjunction extends AbstractBase {

        private final MethodMatcher left, right;

        public Disjunction(MethodMatcher left, MethodMatcher right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return left.matches(methodDescription) || right.matches(methodDescription);
        }
    }

    JunctionMethodMatcher and(MethodMatcher other);

    JunctionMethodMatcher or(MethodMatcher other);
}
