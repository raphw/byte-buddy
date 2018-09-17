package net.bytebuddy.matcher;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * A filterable list allows to use an {@link net.bytebuddy.matcher.ElementMatcher} to reduce a lists to elements
 * that are matched by this matcher in this list.
 *
 * @param <T> The type of the collection's elements.
 * @param <S> The type of this list.
 */
public interface FilterableList<T, S extends FilterableList<T, S>> extends List<T> {

    /**
     * Filters any elements in this lists by the given {@code elementMatcher} and returns a list that are matched
     * by the given matcher.
     *
     * @param elementMatcher The element matcher to match the elements of this list against.
     * @return A new list only containing the matched elements.
     */
    S filter(ElementMatcher<? super T> elementMatcher);

    /**
     * Returns the only element of this list. If there is not exactly one element in this list, an
     * {@link java.lang.IllegalStateException} is thrown.
     *
     * @return The only element of this list.
     */
    T getOnly();

    /**
     * {@inheritDoc}
     */
    S subList(int fromIndex, int toIndex);

    /**
     * An implementation of an empty {@link net.bytebuddy.matcher.FilterableList}.
     *
     * @param <T> The type of the collection's elements.
     * @param <S> The type of this list.
     */
    class Empty<T, S extends FilterableList<T, S>> extends AbstractList<T> implements FilterableList<T, S> {

        /**
         * {@inheritDoc}
         */
        public T get(int index) {
            throw new IndexOutOfBoundsException("index = " + index);
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        public T getOnly() {
            throw new IllegalStateException("size = 0");
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public S filter(ElementMatcher<? super T> elementMatcher) {
            return (S) this;
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public S subList(int fromIndex, int toIndex) {
            if (fromIndex == toIndex && toIndex == 0) {
                return (S) this;
            } else if (fromIndex > toIndex) {
                throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
            } else {
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            }
        }
    }

    /**
     * A base implementation of a {@link net.bytebuddy.matcher.FilterableList}.
     *
     * @param <T> The type of the collection's elements.
     * @param <S> The type of this list.
     */
    abstract class AbstractBase<T, S extends FilterableList<T, S>> extends AbstractList<T> implements FilterableList<T, S> {

        /**
         * A convenience variable indicating the index of a list's only variable.
         */
        private static final int ONLY = 0;

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public S filter(ElementMatcher<? super T> elementMatcher) {
            List<T> filteredElements = new ArrayList<T>(size());
            for (T value : this) {
                if (elementMatcher.matches(value)) {
                    filteredElements.add(value);
                }
            }
            return filteredElements.size() == size() ?
                    (S) this
                    : wrap(filteredElements);
        }

        /**
         * {@inheritDoc}
         */
        public T getOnly() {
            if (size() != 1) {
                throw new IllegalStateException("size = " + size());
            }
            return get(ONLY);
        }

        /**
         * {@inheritDoc}
         */
        public S subList(int fromIndex, int toIndex) {
            return wrap(super.subList(fromIndex, toIndex));
        }

        /**
         * Represents a list of values as an instance of this instance's list type.
         *
         * @param values The values to wrap in an instance of this list's type.
         * @return A wrapped instance of the given {@code values}.
         */
        protected abstract S wrap(List<T> values);
    }
}
