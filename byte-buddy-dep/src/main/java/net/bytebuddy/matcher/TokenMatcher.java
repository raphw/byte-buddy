package net.bytebuddy.matcher;

import net.bytebuddy.description.ByteCodeElement;

public class TokenMatcher<T extends ByteCodeElement.TypeDependant<?, S>, S extends ByteCodeElement.Token<S>> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super S> matcher;

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
