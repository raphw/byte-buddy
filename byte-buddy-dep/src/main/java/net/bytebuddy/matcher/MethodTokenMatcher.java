package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;

/**
 * An element matcher that matches the matched method's method token.
 *
 * @param <T> The type of the matched entity.
 */
public class MethodTokenMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * A matcher to be applied to the matched method's token.
     */
    private final ElementMatcher<? super MethodDescription.Token> matcher;

    /**
     * Creates a new method token matcher.
     *
     * @param matcher A matcher to be applied to the matched method's token.
     */
    public MethodTokenMatcher(ElementMatcher<? super MethodDescription.Token> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.asToken());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((MethodTokenMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "represents(" + matcher + ')';
    }
}
