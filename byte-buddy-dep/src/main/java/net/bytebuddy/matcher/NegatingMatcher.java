package net.bytebuddy.matcher;

/**
 * An element matcher that reverses the matching result of another matcher.
 *
 * @param <T> The type of the matched entity.
 */
public class NegatingMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The element matcher to be negated.
     */
    private final ElementMatcher<? super T> negatedMatcher;

    /**
     * Creates a new negating element matcher.
     *
     * @param negatedMatcher The element matcher to be negated.
     */
    public NegatingMatcher(ElementMatcher<? super T> negatedMatcher) {
        this.negatedMatcher = negatedMatcher;
    }

    @Override
    public boolean matches(T target) {
        return !negatedMatcher.matches(target);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && negatedMatcher.equals(((NegatingMatcher<?>) other).negatedMatcher);
    }

    @Override
    public int hashCode() {
        return -1 * negatedMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "not(" + negatedMatcher + ')';
    }
}
