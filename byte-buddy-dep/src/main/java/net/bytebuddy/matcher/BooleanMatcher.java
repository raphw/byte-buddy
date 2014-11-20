package net.bytebuddy.matcher;

/**
 * Matches a method by a boolean property.
 */
public class BooleanMatcher<T> extends ElementMatcher.Junction.AbstractBase<T>  {

    /**
     * The result of any attempt to match a method.
     */
    private final boolean matches;

    /**
     * Creates a new boolean method matcher.
     *
     * @param matches The result of any attempt to match a method.
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
