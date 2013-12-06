package com.blogspot.mydailyjava.old.method.matcher;

public abstract class JunctionMethodMatcher implements MethodMatcher {

    public static class Conjunction extends JunctionMethodMatcher {

        private final MethodMatcher left, right;

        public Conjunction(MethodMatcher left, MethodMatcher right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches(String classTypeName,
                               String methodName,
                               int methodAccess,
                               String methodDesc,
                               String methodSignature,
                               String[] methodException) {
            return left.matches(classTypeName, methodName, methodAccess, methodDesc, methodSignature, methodException)
                    && right.matches(classTypeName, methodName, methodAccess, methodDesc, methodSignature, methodException);
        }
    }

    public static class Disjunction extends JunctionMethodMatcher {

        private final MethodMatcher left, right;

        public Disjunction(MethodMatcher left, MethodMatcher right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches(String classTypeName,
                               String methodName,
                               int methodAccess,
                               String methodDesc,
                               String methodSignature,
                               String[] methodException) {
            return left.matches(classTypeName, methodName, methodAccess, methodDesc, methodSignature, methodException)
                    || right.matches(classTypeName, methodName, methodAccess, methodDesc, methodSignature, methodException);
        }
    }

    public JunctionMethodMatcher and(MethodMatcher other) {
        return new Conjunction(this, other);
    }

    public JunctionMethodMatcher or(MethodMatcher other) {
        return new Disjunction(this, other);
    }
}
