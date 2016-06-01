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
    private final ElementMatcher<? super T> matcher;

    /**
     * Creates a new negating element matcher.
     *
     * @param matcher The element matcher to be negated.
     */
    public NegatingMatcher(ElementMatcher<? super T> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return !matcher.matches(target);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((NegatingMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return -1 * matcher.hashCode();
    }

    @Override
    public String toString() {
        return "not(" + matcher + ')';
    }
}
