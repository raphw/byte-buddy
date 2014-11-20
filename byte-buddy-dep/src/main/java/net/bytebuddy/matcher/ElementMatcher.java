package net.bytebuddy.matcher;

public interface ElementMatcher<T> {

    boolean matches(T target);

    static interface Junction<S> extends ElementMatcher<S> {

        <U extends S> Junction<U> and(ElementMatcher<? super U> other);

        <U extends S> Junction<U> or(ElementMatcher<? super U> other);

        abstract static class AbstractBase<V> implements Junction<V> {

            @Override
            public <U extends V> Junction<U> and(ElementMatcher<? super U> other) {
                return new Conjunction<U>(this, other);
            }

            @Override
            public <U extends V> Junction<U> or(ElementMatcher<? super U> other) {
                return new Disjunction<U>(this, other);
            }
        }

        static class Conjunction<W> extends AbstractBase<W> {

            private final ElementMatcher<? super W> left, right;

            public Conjunction(ElementMatcher<? super W> left, ElementMatcher<? super W> right) {
                this.left = left;
                this.right = right;
            }

            @Override
            public boolean matches(W target) {
                return left.matches(target) && right.matches(target);
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

        static class Disjunction<W> extends AbstractBase<W> {

            private final ElementMatcher<? super W> left, right;

            public Disjunction(ElementMatcher<? super W> left, ElementMatcher<? super W> right) {
                this.left = left;
                this.right = right;
            }

            @Override
            public boolean matches(W target) {
                return left.matches(target) || right.matches(target);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && left.equals(((Disjunction) other).left)
                        && right.equals(((Disjunction) other).right);
            }

            @Override
            public int hashCode() {
                return 27 * left.hashCode() + right.hashCode();
            }

            @Override
            public String toString() {
                return "(" + left + " or " + right + ')';
            }
        }
    }
}
