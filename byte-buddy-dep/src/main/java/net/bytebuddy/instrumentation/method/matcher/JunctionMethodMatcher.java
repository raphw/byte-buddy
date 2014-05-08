package net.bytebuddy.instrumentation.method.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;

/**
 * A {@link net.bytebuddy.instrumentation.method.matcher.MethodMatcher} that allows to compose
 * a method matcher with another one.
 */
public interface JunctionMethodMatcher extends MethodMatcher {

    /**
     * Creates a new method matcher that returns {@code true} if both this method matcher and the given
     * method matcher match a given method description.
     *
     * @param other The method matcher to compose with this method matcher.
     * @return {@code true} if <b>both</b> this or the {@code other} method matcher returns true.
     */
    JunctionMethodMatcher and(MethodMatcher other);

    /**
     * Creates a new method matcher that returns {@code true} if either this method matcher or the given
     * method matcher match a given method description.
     *
     * @param other The method matcher to compose with this method matcher.
     * @return {@code true} if <b>either</b> this or the {@code other} method matcher returns true.
     */
    JunctionMethodMatcher or(MethodMatcher other);

    /**
     * An abstract base implementation of a junction method matcher.
     */
    abstract static class AbstractBase implements JunctionMethodMatcher {

        @Override
        public JunctionMethodMatcher and(MethodMatcher other) {
            return new Conjunction(this, other);
        }

        @Override
        public JunctionMethodMatcher or(MethodMatcher other) {
            return new Disjunction(this, other);
        }
    }

    /**
     * A conjunction implementation of a method matcher that returns {@code true} if both method matchers match
     * a given method.
     */
    static class Conjunction extends AbstractBase {

        private final MethodMatcher left, right;

        /**
         * Creates a new conjunction method matcher.
         *
         * @param left  The first method matcher to combine within this conjunction.
         * @param right The second method matcher to combine within this conjunction.
         */
        public Conjunction(MethodMatcher left, MethodMatcher right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return left.matches(methodDescription) && right.matches(methodDescription);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && left.equals(((Conjunction) other).left)
                    && right.equals(((Conjunction) other).right);
        }

        @Override
        public int hashCode() {
            return 31 * left.hashCode() + right.hashCode();
        }

        @Override
        public String toString() {
            return "(" + left + " and " + right + ')';
        }
    }

    /**
     * A disjunction implementation of a method matcher that returns {@code true} if either of two method matchers
     * matches a given method.
     */
    static class Disjunction extends AbstractBase {

        private final MethodMatcher left, right;

        /**
         * Creates a new disjunction method matcher.
         *
         * @param left  The first method matcher to combine within this disjunction.
         * @param right The second method matcher to combine within this disjunction.
         */
        public Disjunction(MethodMatcher left, MethodMatcher right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return left.matches(methodDescription) || right.matches(methodDescription);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && left.equals(((Disjunction) other).left)
                    && right.equals(((Disjunction) other).right);
        }

        @Override
        public int hashCode() {
            return 31 * left.hashCode() + right.hashCode();
        }

        @Override
        public String toString() {
            return "(" + left + " or " + right + ')';
        }
    }
}
