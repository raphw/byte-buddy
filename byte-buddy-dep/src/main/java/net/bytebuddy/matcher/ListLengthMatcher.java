package net.bytebuddy.matcher;

import java.util.List;

/**
 * An element matcher that matches a list for its length.
 *
 * @param <T> The type of the matched entity.
 */
public class ListLengthMatcher<T extends List<?>> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The expected length of the matched list.
     */
    private final int length;

    /**
     * Creates a new matcher that matches the size of a matched list.
     *
     * @param length The expected length of the matched list.
     */
    public ListLengthMatcher(int length) {
        this.length = length;
    }

    @Override
    public boolean matches(T target) {
        return target.size() == length;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && length == ((ListLengthMatcher) other).length;
    }

    @Override
    public int hashCode() {
        return length;
    }

    @Override
    public String toString() {
        return "length(" + length + ')';
    }
}
