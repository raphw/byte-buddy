package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotatedElement;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;

import java.util.List;

public class AnnotationMatcher<T extends AnnotatedElement> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super List<? extends AnnotationDescription>> annotationTypeMatcher;

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
        return "annotation(" + annotationTypeMatcher + ')';
    }
}
