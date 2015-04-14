package net.bytebuddy.matcher;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches the list of inherited annotations of a type description.
 *
 * @param <T> The actual matched type of this matcher.
 */
public class InheritedAnnotationMatcher<T extends TypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to be applied to the provided annotation list.
     */
    private final ElementMatcher<? super AnnotationList> annotationMatcher;

    /**
     * Creates a new matcher for the inherited annotations of a type description.
     *
     * @param annotationMatcher The matcher to be applied to the provided annotation list.
     */
    public InheritedAnnotationMatcher(ElementMatcher<? super AnnotationList> annotationMatcher) {
        this.annotationMatcher = annotationMatcher;
    }

    @Override
    public boolean matches(T target) {
        return annotationMatcher.matches(target.getInheritedAnnotations());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && annotationMatcher.equals(((InheritedAnnotationMatcher<?>) other).annotationMatcher);
    }

    @Override
    public int hashCode() {
        return annotationMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "inheritsAnnotations(" + annotationMatcher + ")";
    }
}
