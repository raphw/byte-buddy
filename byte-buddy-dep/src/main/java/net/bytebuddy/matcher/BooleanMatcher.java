package net.bytebuddy.matcher;

/**
 * An element matcher that returns a fixed result.
 *
 * @param <T> The actual matched type of this matcher.
 */
public class BooleanMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The predefined result.
     */
    private final boolean matches;

    /**
     * Creates a new boolean element matcher.
     *
     * @param matches The predefined result.
     */
    public BooleanMatcher(boolean matches) {
        this.matches = matches;
    }

    @Override
    public boolean matches(T target) {
        return matches;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matches == ((BooleanMatcher) other).matches;
    }

    @Override
    public int hashCode() {
        return (matches ? 1 : 0);
    }

    @Override
    public String toString() {
        return Boolean.toString(matches);
    }
}
