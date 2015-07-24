package net.bytebuddy.matcher;

import net.bytebuddy.description.field.FieldDescription;

public class FieldShapeMatcher<T extends FieldDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super FieldDescription.InDefinedShape> matcher;

    public FieldShapeMatcher(ElementMatcher<? super FieldDescription.InDefinedShape> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.asDefined());
    }
}
