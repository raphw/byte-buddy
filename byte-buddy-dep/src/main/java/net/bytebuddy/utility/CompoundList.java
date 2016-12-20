package net.bytebuddy.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates a list representation of two lists as a single, compound list.
 */
public class CompoundList {

    /**
     * A compund list cannot be created.
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
        List<S> list = new ArrayList<S>(left.size() + right.size());
        list.addAll(left);
        list.addAll(right);
        return list;
    }
}
