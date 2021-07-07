package net.bytebuddy.matcher;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * A filterable list allows to use an {@link net.bytebuddy.matcher.ElementMatcher} to reduce a lists to elements
 * that are matched by this matcher in this list. 可过滤列表允许使用 {@link net.bytebuddy.matcher.ElementMatcher} 将列表缩减为与此列表中的匹配器匹配的元素
 *
 * @param <T> The type of the collection's elements.
 * @param <S> The type of this list.
 */
public interface FilterableList<T, S extends FilterableList<T, S>> extends List<T> {

    /**
     * Filters any elements in this lists by the given {@code elementMatcher} and returns a list that are matched
     * by the given matcher. 按给定的 {@code elementMatcher} 筛选此列表中的任何元素，并返回由给定匹配器匹配的列表
     *
     * @param elementMatcher The element matcher to match the elements of this list against.
     * @return A new list only containing the matched elements.
     */
    S filter(ElementMatcher<? super T> elementMatcher);

    /**
     * Returns the only element of this list. If there is not exactly one element in this list, an
     * {@link java.lang.IllegalStateException} is thrown. 返回此列表的唯一元素。如果列表中没有一个元素，{@link java.lang.IllegalStateException} 被抛出
     *
     * @return The only element of this list.
     */
    T getOnly();

    @Override
    S subList(int fromIndex, int toIndex);

    /**
     * An implementation of an empty {@link net.bytebuddy.matcher.FilterableList}. 一个空的{@link net.bytebuddy.matcher.FilterableList}的实现
     *
     * @param <T> The type of the collection's elements.
     * @param <S> The type of this list.
     */
    class Empty<T, S extends FilterableList<T, S>> extends AbstractList<T> implements FilterableList<T, S> {

        @Override
        public T get(int index) {
            throw new IndexOutOfBoundsException("index = " + index);
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public T getOnly() {
            throw new IllegalStateException("size = 0");
        }

        @Override
        @SuppressWarnings("unchecked")
        public S filter(ElementMatcher<? super T> elementMatcher) {
            return (S) this;
        }

        @Override
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
     * A base implementation of a {@link net.bytebuddy.matcher.FilterableList}. {@link net.bytebuddy.matcher.FilterableList}的基本实现
     *
     * @param <T> The type of the collection's elements.
     * @param <S> The type of this list.
     */
    abstract class AbstractBase<T, S extends FilterableList<T, S>> extends AbstractList<T> implements FilterableList<T, S> {

        /**
         * A convenience variable indicating the index of a list's only variable. 一个方便变量，表示列表中唯一变量的索引
         */
        private static final int ONLY = 0;

        @Override
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

        @Override
        public T getOnly() {
            if (size() != 1) {
                throw new IllegalStateException("size = " + size());
            }
            return get(ONLY);
        }

        @Override
        public S subList(int fromIndex, int toIndex) {
            return wrap(super.subList(fromIndex, toIndex));
        }

        /**
         * Represents a list of values as an instance of this instance's list type. 将值列表表示为此实例的列表类型的实例
         *
         * @param values The values to wrap in an instance of this list's type.
         * @return A wrapped instance of the given {@code values}.
         */
        protected abstract S wrap(List<T> values);
    }
}
