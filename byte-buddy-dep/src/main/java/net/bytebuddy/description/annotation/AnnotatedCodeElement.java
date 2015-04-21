package net.bytebuddy.description.annotation;

/**
 * Describes an element that declares annotations.
 */
public interface AnnotatedCodeElement {

    /**
     * Returns a list of annotations that are declared by this instance.
     *
     * @return A list of declared annotations.
     */
    AnnotationList getDeclaredAnnotations();
}
