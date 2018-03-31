package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

/**
 * A fail-safe matcher catches exceptions that are thrown by a delegate matcher and returns an alternative value.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
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
    public String toString() {
        return "failSafe(try(" + matcher + ") or " + fallback + ")";
    }
}
