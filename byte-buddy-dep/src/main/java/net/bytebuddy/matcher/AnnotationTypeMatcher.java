package net.bytebuddy.matcher;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches the type of an annotation description.
 *
 * @param <T> The exact type of the annotation description that is matched.
 */
public class AnnotationTypeMatcher<T extends AnnotationDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type matcher to apply to an annotation's type.
     */
    private final ElementMatcher<? super TypeDescription> typeMatcher;

    /**
     * Creates a new matcher for an annotation description's type.
     *
     * @param typeMatcher The type matcher to apply to an annotation's type.
     */
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
                && typeMatcher.equals(((AnnotationTypeMatcher<?>) other).typeMatcher);
    }

    @Override
    public int hashCode() {
        return typeMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "ofAnnotationType(" + typeMatcher + ')';
    }
}
