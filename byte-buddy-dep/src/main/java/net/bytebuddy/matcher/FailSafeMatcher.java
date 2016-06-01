package net.bytebuddy.matcher;

/**
 * A fail-safe matcher catches exceptions that are thrown by a delegate matcher and returns an alternative value.
 *
 * @param <T> The type of the matched entity.
 */
public class FailSafeMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The delegate matcher that might throw an exception.
     */
    private final ElementMatcher<? super T> matcher;

    /**
     * The fallback value in case of an exception.
     */
    private final boolean fallback;

    /**
     * Creates a new fail-safe element matcher.
     *
     * @param matcher  The delegate matcher that might throw an exception.
     * @param fallback The fallback value in case of an exception.
     */
    public FailSafeMatcher(ElementMatcher<? super T> matcher, boolean fallback) {
        this.matcher = matcher;
        this.fallback = fallback;
    }

    @Override
    public boolean matches(T target) {
        try {
            return matcher.matches(target);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        FailSafeMatcher<?> that = (FailSafeMatcher<?>) other;
        return fallback == that.fallback && matcher.equals(that.matcher);
    }

    @Override
    public int hashCode() {
        int result = matcher.hashCode();
        result = 31 * result + (fallback ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "failSafe(try(" + matcher + ") or " + fallback + ")";
    }
}
