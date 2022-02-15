/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.nullability.UnknownNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
     * @param target The instance to be matched or {@code null}.
     * @return {@code true} if the given element is matched by this matcher or {@code false} otherwise.
     */
    boolean matches(@UnknownNull T target);

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

            /**
             * {@inheritDoc}
             */
            @SuppressWarnings("unchecked") // In absence of @SafeVarargs
            public <U extends V> Junction<U> and(ElementMatcher<? super U> other) {
                return new Conjunction<U>(this, other);
            }

            /**
             * {@inheritDoc}
             */
            @SuppressWarnings("unchecked") // In absence of @SafeVarargs
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
            private final List<ElementMatcher<? super W>> matchers;

            /**
             * Creates a new conjunction matcher.
             *
             * @param matcher The represented matchers in application order.
             */
            @SuppressWarnings("unchecked") // In absence of @SafeVarargs
            public Conjunction(ElementMatcher<? super W>... matcher) {
                this(Arrays.asList(matcher));
            }

            /**
             * Creates a new conjunction matcher.
             *
             * @param matchers The represented matchers in application order.
             */
            @SuppressWarnings("unchecked")
            public Conjunction(List<ElementMatcher<? super W>> matchers) {
                this.matchers = new ArrayList<ElementMatcher<? super W>>(matchers.size());
                for (ElementMatcher<? super W> matcher : matchers) {
                    if (matcher instanceof Conjunction<?>) {
                        this.matchers.addAll(((Conjunction<Object>) matcher).matchers);
                    } else {
                        this.matchers.add(matcher);
                    }
                }
            }

            /**
             * {@inheritDoc}
             */
            public boolean matches(@UnknownNull W target) {
                for (ElementMatcher<? super W> matcher : matchers) {
                    if (!matcher.matches(target)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String toString() {
                StringBuilder stringBuilder = new StringBuilder("(");
                boolean first = true;
                for (ElementMatcher<? super W> matcher : matchers) {
                    if (first) {
                        first = false;
                    } else {
                        stringBuilder.append(" and ");
                    }
                    stringBuilder.append(matcher);
                }
                return stringBuilder.append(")").toString();
            }
        }

        /**
         * A disjunction matcher which matches an element against matchers in order to constitute a successful match.
         *
         * @param <W> The type of the object that is being matched.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Disjunction<W> extends AbstractBase<W> {

            /**
             * The element matchers that constitute this disjunction.
             */
            private final List<ElementMatcher<? super W>> matchers;

            /**
             * Creates a new disjunction matcher.
             *
             * @param matcher The represented matchers in application order.
             */
            @SuppressWarnings("unchecked") // In absence of @SafeVarargs
            public Disjunction(ElementMatcher<? super W>... matcher) {
                this(Arrays.asList(matcher));
            }

            /**
             * Creates a new disjunction matcher.
             *
             * @param matchers The represented matchers in application order.
             */
            @SuppressWarnings("unchecked")
            public Disjunction(List<ElementMatcher<? super W>> matchers) {
                this.matchers = new ArrayList<ElementMatcher<? super W>>(matchers.size());
                for (ElementMatcher<? super W> matcher : matchers) {
                    if (matcher instanceof Disjunction<?>) {
                        this.matchers.addAll(((Disjunction<Object>) matcher).matchers);
                    } else {
                        this.matchers.add(matcher);
                    }
                }
            }

            /**
             * {@inheritDoc}
             */
            public boolean matches(@UnknownNull W target) {
                for (ElementMatcher<? super W> matcher : matchers) {
                    if (matcher.matches(target)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                StringBuilder stringBuilder = new StringBuilder("(");
                boolean first = true;
                for (ElementMatcher<? super W> matcher : matchers) {
                    if (first) {
                        first = false;
                    } else {
                        stringBuilder.append(" or ");
                    }
                    stringBuilder.append(matcher);
                }
                return stringBuilder.append(")").toString();
            }
        }

        /**
         * An abstract base implementation that rejects null values.
         *
         * @param <W> The type of the object that is being matched.
         */
        @HashCodeAndEqualsPlugin.Enhance
        abstract class ForNonNullValues<W> extends AbstractBase<W> {

            /**
             * {@inheritDoc}
             */
            public boolean matches(@MaybeNull W target) {
                return target != null && doMatch(target);
            }

            /**
             * Matches the supplied value if it was found not to be {@code null}.
             *
             * @param target The instance to be matched.
             * @return {@code true} if the given element is matched by this matcher or {@code false} otherwise.
             */
            protected abstract boolean doMatch(W target);
        }
    }
}
