package net.bytebuddy.utility;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * A list representation of two lists as a single, compound list.
 *
 * @param <T> The type of the list's elements.
 */
public class CompoundList<T> extends AbstractList<T> {

    /**
     * The left list.
     */
    private final List<? extends T> left;

    /**
     * The right list.
     */
    private final List<? extends T> right;

    /**
     * Creates a new compound list.
     *
     * @param left  The left list.
     * @param right The right list.
     */
    protected CompoundList(List<? extends T> left, List<? extends T> right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Creates a list of a single element and another list.
     *
     * @param left  The left element.
     * @param right The right list.
     * @param <S>   The type of the list's elements.
     * @return A compound list representing the element and the list.
     */
    public static <S> List<S> of(S left, List<? extends S> right) {
        return of(Collections.singletonList(left), right);
    }

    /**
     * Creates a list of a list and an element.
     *
     * @param left  The left left.
     * @param right The right element.
     * @param <S>   The type of the list's elements.
     * @return A compound list representing the element and the list.
     */
    public static <S> List<S> of(List<? extends S> left, S right) {
        return of(left, Collections.singletonList(right));
    }

    /**
     * Creates a list of a left and right list.
     *
     * @param left  The left list.
     * @param right The right list.
     * @param <S>   The type of the list's elements.
     * @return A compound list representing the element and the list.
     */
    public static <S> List<S> of(List<? extends S> left, List<? extends S> right) {
        return new CompoundList<S>(left, right);
    }

    @Override
    public T get(int index) {
        int leftSize = left.size();
        return leftSize - index > 0
                ? (T) left.get(index)
                : (T) right.get(index - leftSize);
    }

    @Override
    public int size() {
        return left.size() + right.size();
    }
}
