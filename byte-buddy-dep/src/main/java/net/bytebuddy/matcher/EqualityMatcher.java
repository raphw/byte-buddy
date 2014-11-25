package net.bytebuddy.matcher;

/**
 * An element matcher that checks an object's equality to another object.
 *
 * @param <T> The type of the matched entity.
 */
public class EqualityMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The object that is checked to be equal to the matched value.
     */
    private final Object value;

    /**
     * Creates an element matcher that tests for equality.
     *
     * @param value The object that is checked to be equal to the matched value.
     */
    public EqualityMatcher(Object value) {
        this.value = value;
    }

    @Override
    public boolean matches(T target) {
        return value.equals(target);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && value.equals(((EqualityMatcher) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "is(" + value + ")";
    }
}
