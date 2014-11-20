package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;

public class AnnotationTypeMatcher<T extends AnnotationDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super TypeDescription> typeMatcher;

    public AnnotationTypeMatcher(ElementMatcher<? super TypeDescription> typeMatcher) {
        this.typeMatcher = typeMatcher;
    }

    @Override
    public boolean matches(T target) {
        return typeMatcher.matches(target.getAnnotationType());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && typeMatcher.equals(((AnnotationTypeMatcher) other).typeMatcher);
    }

    @Override
    public int hashCode() {
        return typeMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "annotationType(" + typeMatcher + ')';
    }
}
