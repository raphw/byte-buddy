package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotatedElement;

import java.lang.annotation.Annotation;

/**
 * Matches a method by its annotations.
 */
class AnnotationMatcher extends ElementMatcher.Junction.AbstractBase<AnnotatedElement> {

    /**
     * The annotation type to match to be present on a method.
     */
    private final Class<? extends Annotation> annotationType;

    /**
     * Creates a new annotation method matcher.
     *
     * @param annotationType The annotation type to match to be present on a method.
     */
    AnnotationMatcher(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public boolean matches(AnnotatedElement target) {
        return target.getDeclaredAnnotations().isAnnotationPresent(annotationType);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && annotationType.equals(((AnnotationMatcher) other).annotationType);
    }

    @Override
    public int hashCode() {
        return annotationType.hashCode();
    }

    @Override
    public String toString() {
        return "isAnnotatedBy(" + annotationType.getName() + ')';
    }
}
