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
}
