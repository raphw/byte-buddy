package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

/**
 * An element matcher is used as a predicate for identifying code elements such as types, methods, fields or
 * annotations. They are similar to Java 8's {@code Predicate}s but compatible to Java 6 and Java 7 and represent
 * a functional interface. They can be chained by using instances of
 * {@link net.bytebuddy.matcher.ElementMatcher.Junction}.
 *
 * @param <T> The type of the object that is being matched.
 */
public interface ElementMatcher<T> {

    /**
     * Matches a target against this element matcher.
     *
     * @param target The instance to be matched.
     * @return {@code true} if the given element is matched by this matcher or {@code false} otherwise.
     */
    boolean matches(T target);

    /**
     * A junctions allows to chain different {@link net.bytebuddy.matcher.ElementMatcher}s in a readable manner.
     *
     * @param <S> The type of the object that is being matched.
     */
    interface Junction<S> extends ElementMatcher<S> {

        /**
         * Creates a conjunction where this matcher and the {@code other} matcher must both be matched in order
         * to constitute a successful match. The other matcher is only invoked if this matcher constitutes a successful
         * match.
         *
         * @param other The second matcher to consult.
         * @param <U>   The type of the object that is being matched. Note that Java's type inference might not
         *              be able to infer the common subtype of this instance and the {@code other} matcher such that
         *              this type must need to be named explicitly.
         * @return A conjunction of this matcher and the other matcher.
         */
        <U extends S> Junction<U> and(ElementMatcher<? super U> other);

        /**
         * Creates a disjunction where either this matcher or the {@code other} matcher must be matched in order
         * to constitute a successful match. The other matcher is only invoked if this matcher constitutes an
         * unsuccessful match.
         *
         * @param other The second matcher to consult.
         * @param <U>   The type of the object that is being matched. Note that Java's type inference might not
         *              be able to infer the common subtype of this instance and the {@code other} matcher such that
         *              this type must need to be named explicitly.
         * @return A disjunction of this matcher and the other matcher.
         */
        <U extends S> Junction<U> or(ElementMatcher<? super U> other);

        /**
         * A base implementation of {@link net.bytebuddy.matcher.ElementMatcher.Junction}.
         *
         * @param <V> The type of the object that is being matched.
         */
        abstract class AbstractBase<V> implements Junction<V> {

            @Override
            public <U extends V> Junction<U> and(ElementMatcher<? super U> other) {
                return new Conjunction<U>(this, other);
            }

            @Override
            public <U extends V> Junction<U> or(ElementMatcher<? super U> other) {
                return new Disjunction<U>(this, other);
            }
        }

        /**
         * A conjunction matcher which only matches an element if both represented matchers constitute a match.
         *
         * @param <W> The type of the object that is being matched.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Conjunction<W> extends AbstractBase<W> {

            /**
             * The element matchers that constitute this conjunction.
             */
            private final ElementMatcher<? super W> left, right;

            /**
             * Creates a new conjunction matcher.
             *
             * @param left  The first matcher to consult for a match.
             * @param right The second matcher to consult for a match. This matcher is only consulted
             *              if the {@code first} matcher constituted a match.
             */
            public Conjunction(ElementMatcher<? super W> left, ElementMatcher<? super W> right) {
                this.left = left;
                this.right = right;
            }

            @Override
            public boolean matches(W target) {
                return left.matches(target) && right.matches(target);
            }

            @Override
            public String toString() {
                return "(" + left + " and " + right + ')';
            }
        }

        /**
         * A disjunction matcher which only matches an element if both represented matchers constitute a match.
         *
         * @param <W> The type of the object that is being matched.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Disjunction<W> extends AbstractBase<W> {

            /**
             * The element matchers that constitute this disjunction.
             */
            private final ElementMatcher<? super W> left, right;

            /**
             * Creates a new disjunction matcher.
             *
             * @param left  The first matcher to consult for a match.
             * @param right The second matcher to consult for a match. This matcher is only consulted
             *              if the {@code first} matcher did not already constitute a match.
             */
            public Disjunction(ElementMatcher<? super W> left, ElementMatcher<? super W> right) {
                this.left = left;
                this.right = right;
            }

            @Override
            public boolean matches(W target) {
                return left.matches(target) || right.matches(target);
            }

            @Override
            public String toString() {
                return "(" + left + " or " + right + ')';
            }
        }
    }
}
