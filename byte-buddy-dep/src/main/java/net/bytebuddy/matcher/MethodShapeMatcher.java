package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;

public class MethodShapeMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super MethodDescription.InDefinedShape> matcher;

    public MethodShapeMatcher(ElementMatcher<? super MethodDescription.InDefinedShape> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.asDefined());
    }
}
