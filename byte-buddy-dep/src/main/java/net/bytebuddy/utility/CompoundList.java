package net.bytebuddy.utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a list representation of two lists as a single, compound list.
 */
public class CompoundList {

    /**
     * A compound list cannot be created.
     */
    private CompoundList() {
        throw new UnsupportedOperationException("Cannot create a compound list");
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
        List<S> list = new ArrayList<S>(right.size() + 1);
        list.add(left);
        if (!right.isEmpty()) {
            list.addAll(right);
        }
        return list;
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
        List<S> list = new ArrayList<S>(left.size() + 1);
        if (!left.isEmpty()) {
            list.addAll(left);
        }
        list.add(right);
        return list;
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
        List<S> list = new ArrayList<S>(left.size() + right.size());
        if (!left.isEmpty()) {
            list.addAll(left);
        }
        if (!right.isEmpty()) {
            list.addAll(right);
        }
        return list;
    }
}
