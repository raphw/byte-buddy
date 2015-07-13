package net.bytebuddy.matcher;

import net.bytebuddy.description.method.ParameterDescription;

/**
 * An element matcher that matches the matched parameter's token.
 *
 * @param <T> The type of the matched entity.
 */
public class ParameterTokenMatcher<T extends ParameterDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * A matcher to be applied to the matched parameter's token.
     */
    private final ElementMatcher<? super ParameterDescription.Token> matcher;

    /**
     * Creates a new field token matcher.
     *
     * @param matcher A matcher to be applied to the matched parameter's token.
     */
    public ParameterTokenMatcher(ElementMatcher<? super ParameterDescription.Token> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.asToken());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((ParameterTokenMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "representedBy(" + matcher + ')';
    }
}
