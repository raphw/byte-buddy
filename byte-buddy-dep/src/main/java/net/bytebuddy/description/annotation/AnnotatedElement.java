package net.bytebuddy.description.annotation;

/**
 * Describes an element that declares annotations.
 */
public interface AnnotatedElement {

    /**
     * Returns a list of annotations that are declared by this instance.
     *
     * @return A list of declared annotations.
     */
    AnnotationList getDeclaredAnnotations();
}
