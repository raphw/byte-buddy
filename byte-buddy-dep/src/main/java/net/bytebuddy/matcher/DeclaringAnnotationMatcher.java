package net.bytebuddy.matcher;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.annotation.AnnotatedCodeElement;
import net.bytebuddy.description.annotation.AnnotationList;

/**
 * An element matcher that matches the list of annotations that are provided by an annotated element.
 *
 * @param <T> The actual matched type of this matcher.
 */
@EqualsAndHashCode(callSuper = false)
public class DeclaringAnnotationMatcher<T extends AnnotatedCodeElement> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to be applied to the provided annotation list.
     */
    private final ElementMatcher<? super AnnotationList> matcher;

    /**
     * Creates a new matcher for the annotations of an annotated element.
     *
     * @param matcher The matcher to be applied to the provided annotation list.
     */
    public DeclaringAnnotationMatcher(ElementMatcher<? super AnnotationList> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getDeclaredAnnotations());
    }

    @Override
    public String toString() {
        return "declaresAnnotations(" + matcher + ")";
    }
}
