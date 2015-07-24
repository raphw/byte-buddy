package net.bytebuddy.matcher;

import net.bytebuddy.description.ByteCodeElement;

public class DefinedShapeMatcher<T extends ByteCodeElement.TypeDependant<S, ?>, S extends ByteCodeElement.TypeDependant<?, ?>>
        extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super S> matcher;

    public DefinedShapeMatcher(ElementMatcher<? super S> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.asDefined());
    }
}
