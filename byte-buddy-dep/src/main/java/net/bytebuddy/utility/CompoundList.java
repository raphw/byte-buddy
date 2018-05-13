package net.bytebuddy.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates a list representation of two lists as a single, compound list.
 */
public class CompoundList {

    /**
     * A compound list cannot be created.
     */
    private CompoundList() {
        throw new UnsupportedOperationException("This class is a utility class and not supposed to be instantiated");
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
        if (right.isEmpty()) {
            return Collections.singletonList(left);
        } else {
            List<S> list = new ArrayList<S>(1 + right.size());
            list.add(left);
            list.addAll(right);
            return list;
        }
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
        if (left.isEmpty()) {
            return Collections.singletonList(right);
        } else {
            List<S> list = new ArrayList<S>(left.size() + 1);
            list.addAll(left);
            list.add(right);
            return list;
        }
    }

    /**
     * Creates a list of a left and right list.
     *
     * @param left  The left list.
     * @param right The right list.
     * @param <S>   The type of the list's elements.
     * @return A compound list representing the elements of both lists.
     */
    public static <S> List<S> of(List<? extends S> left, List<? extends S> right) {
        List<S> list = new ArrayList<S>(left.size() + right.size());
        list.addAll(left);
        list.addAll(right);
        return list;
    }


    /**
     * Creates a list of a left, a middle and a right list.
     *
     * @param left   The left list.
     * @param middle The middle list.
     * @param right  The right list.
     * @param <S>    The type of the list's elements.
     * @return A compound list representing the elements of all lists.
     */
    public static <S> List<S> of(List<? extends S> left, List<? extends S> middle, List<? extends S> right) {
        List<S> list = new ArrayList<S>(left.size() + middle.size() + right.size());
        list.addAll(left);
        list.addAll(middle);
        list.addAll(right);
        return list;
    }
}
