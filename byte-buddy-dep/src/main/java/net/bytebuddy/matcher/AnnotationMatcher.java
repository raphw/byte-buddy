package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotatedElement;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;

import java.util.List;

/**
 * An element matcher that matches the annotations that are declared by an annotated element.
 *
 * @param <T> The exact type of the annotated element that is matched.
 */
public class AnnotationMatcher<T extends AnnotatedElement> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The element matcher to match the declared annotations against.
     */
    private final ElementMatcher<? super List<? extends AnnotationDescription>> annotationTypeMatcher;

    /**
     * Creates a new matcher for the declared annotations of an annotated element.
     *
     * @param annotationTypeMatcher The element matcher to match the declared annotations against.
     */
    public AnnotationMatcher(ElementMatcher<? super List<? extends AnnotationDescription>> annotationTypeMatcher) {
        this.annotationTypeMatcher = annotationTypeMatcher;
    }

    @Override
    public boolean matches(T target) {
        return annotationTypeMatcher.matches(target.getDeclaredAnnotations());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && annotationTypeMatcher.equals(((AnnotationMatcher) other).annotationTypeMatcher);
    }

    @Override
    public int hashCode() {
        return annotationTypeMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "hasAnnotation(" + annotationTypeMatcher + ')';
    }
}
