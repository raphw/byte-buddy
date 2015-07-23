package net.bytebuddy.matcher;

import net.bytebuddy.description.TypeDefinable;

public class DefinedShapeMatcher<T extends TypeDefinable<?, ? extends S>, S extends T> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super T> matcher;

    public DefinedShapeMatcher(ElementMatcher<? super T> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.asDefined());
    }
}
