package net.bytebuddy.matcher;

/**
 * Matches a method by negating another method matcher.
 */
class NegatingMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The method matcher to negate.
     */
    private final ElementMatcher<T> negatedMatcher;

    /**
     * Creates a new negating method matcher.
     *
     * @param negatedMatcher The method matcher to negate.
     */
    public NegatingMatcher(ElementMatcher<T> negatedMatcher) {
        this.negatedMatcher = negatedMatcher;
    }

    @Override
    public boolean matches(T target) {
        return !negatedMatcher.matches(target);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && negatedMatcher.equals(((NegatingMatcher) other).negatedMatcher);
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
