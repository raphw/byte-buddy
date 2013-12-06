package com.blogspot.mydailyjava.bytebuddy.interception.method.matching;

import com.blogspot.mydailyjava.bytebuddy.extraction.information.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.information.MethodContext;

public abstract class JunctionMethodMatcher implements MethodMatcher {

    public static class Conjunction extends JunctionMethodMatcher {

        private final MethodMatcher left, right;

        public Conjunction(MethodMatcher left, MethodMatcher right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches(ClassContext classContext, MethodContext methodContext) {
            return left.matches(classContext, methodContext) && right.matches(classContext, methodContext);
        }
    }

    public static class Disjunction extends JunctionMethodMatcher {

        private final MethodMatcher left, right;

        public Disjunction(MethodMatcher left, MethodMatcher right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches(ClassContext classContext, MethodContext methodContext) {
            return left.matches(classContext, methodContext) || right.matches(classContext, methodContext);
        }
    }

    public JunctionMethodMatcher and(MethodMatcher other) {
        return new Conjunction(this, other);
    }

    public JunctionMethodMatcher or(MethodMatcher other) {
        return new Disjunction(this, other);
    }
}
