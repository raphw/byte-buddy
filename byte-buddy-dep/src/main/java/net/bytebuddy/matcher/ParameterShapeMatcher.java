package net.bytebuddy.matcher;

import net.bytebuddy.description.method.ParameterDescription;

public class ParameterShapeMatcher<T extends ParameterDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super ParameterDescription.InDefinedShape> matcher;

    public ParameterShapeMatcher(ElementMatcher<? super ParameterDescription.InDefinedShape> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.asDefined());
    }
}
