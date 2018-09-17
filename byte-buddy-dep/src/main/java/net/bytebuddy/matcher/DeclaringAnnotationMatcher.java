package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;

/**
 * An element matcher that matches the list of annotations that are provided by an annotated element.
 *
 * @param <T> The actual matched type of this matcher.
 */
@HashCodeAndEqualsPlugin.Enhance
public class DeclaringAnnotationMatcher<T extends AnnotationSource> extends ElementMatcher.Junction.AbstractBase<T> {

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

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matcher.matches(target.getDeclaredAnnotations());
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "declaresAnnotations(" + matcher + ")";
    }
}
