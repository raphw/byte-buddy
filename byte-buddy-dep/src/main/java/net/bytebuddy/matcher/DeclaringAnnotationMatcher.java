package net.bytebuddy.matcher;

import net.bytebuddy.description.annotation.AnnotatedCodeElement;
import net.bytebuddy.description.annotation.AnnotationList;

/**
 * An element matcher that matches the list of annotations that are provided by an annotated element.
 *
 * @param <T> The actual matched type of this matcher.
 */
public class DeclaringAnnotationMatcher<T extends AnnotatedCodeElement> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to be applied to the provided annotation list.
     */
    private final ElementMatcher<? super AnnotationList> annotationMatcher;

    /**
     * Creates a new matcher for the annotations of an annotated element.
     *
     * @param annotationMatcher The matcher to be applied to the provided annotation list.
     */
    public DeclaringAnnotationMatcher(ElementMatcher<? super AnnotationList> annotationMatcher) {
        this.annotationMatcher = annotationMatcher;
    }

    @Override
    public boolean matches(T target) {
        return annotationMatcher.matches(target.getDeclaredAnnotations());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && annotationMatcher.equals(((DeclaringAnnotationMatcher<?>) other).annotationMatcher);
    }

    @Override
    public int hashCode() {
        return annotationMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "declaresAnnotations(" + annotationMatcher + ")";
    }
}
