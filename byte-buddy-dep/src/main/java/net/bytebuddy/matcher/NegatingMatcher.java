package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

/**
 * An element matcher that reverses the matching result of another matcher.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
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

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return !matcher.matches(target);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "not(" + matcher + ')';
    }
}
