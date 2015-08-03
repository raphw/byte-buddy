package net.bytebuddy.matcher;

import net.bytebuddy.description.ByteCodeElement;

/**
 * An element matcher that matches a byte code's element's token against a matcher for such a token.
 *
 * @param <T> The type of the matched entity.
 * @param <S> The type of the matched token.
 */
public class TokenMatcher<T extends ByteCodeElement.TypeDependant<?, S>, S extends ByteCodeElement.Token<S>> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the token of the matched byte code element.
     */
    private final ElementMatcher<? super S> matcher;

    /**
     * Creates a new token matcher.
     *
     * @param matcher The matcher to apply to the token of the matched byte code element.
     */
    public TokenMatcher(ElementMatcher<? super S> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.asToken());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((TokenMatcher<?, ?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "hasToken(" + matcher + ')';
    }
}
