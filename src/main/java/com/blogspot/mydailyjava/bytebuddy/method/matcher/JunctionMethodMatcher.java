package com.blogspot.mydailyjava.bytebuddy.method.matcher;


import java.lang.reflect.Method;

public abstract class JunctionMethodMatcher implements MethodMatcher {

    public static class Conjunction extends JunctionMethodMatcher {

        private final MethodMatcher left, right;

        public Conjunction(MethodMatcher left, MethodMatcher right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches(Method method) {
            return left.matches(method) && right.matches(method);
        }
    }

    public static class Disjunction extends JunctionMethodMatcher {

        private final MethodMatcher left, right;

        public Disjunction(MethodMatcher left, MethodMatcher right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches(Method method) {
            return left.matches(method) || right.matches(method);
        }
    }

    public JunctionMethodMatcher and(MethodMatcher other) {
        return new Conjunction(this, other);
    }

    public JunctionMethodMatcher or(MethodMatcher other) {
        return new Disjunction(this, other);
    }
}
