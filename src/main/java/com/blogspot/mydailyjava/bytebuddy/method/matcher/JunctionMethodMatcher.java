package com.blogspot.mydailyjava.bytebuddy.method.matcher;


import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;

public abstract class JunctionMethodMatcher implements MethodMatcher {

    public static class Conjunction extends JunctionMethodMatcher {

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

    public static class Disjunction extends JunctionMethodMatcher {

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

    public JunctionMethodMatcher and(MethodMatcher other) {
        return new Conjunction(this, other);
    }

    public JunctionMethodMatcher or(MethodMatcher other) {
        return new Disjunction(this, other);
    }
}
