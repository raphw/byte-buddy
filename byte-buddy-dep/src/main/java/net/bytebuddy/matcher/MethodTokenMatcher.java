package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;

import static net.bytebuddy.matcher.ElementMatchers.none;

public class MethodTokenMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super MethodDescription.Token> matcher;

    public MethodTokenMatcher(ElementMatcher<? super MethodDescription.Token> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.asToken(none()));
    }
}
