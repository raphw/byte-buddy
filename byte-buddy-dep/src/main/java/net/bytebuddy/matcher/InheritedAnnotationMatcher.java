package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches the list of inherited annotations of a type description.
 *
 * @param <T> The actual matched type of this matcher.
 */
@HashCodeAndEqualsPlugin.Enhance
public class InheritedAnnotationMatcher<T extends TypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to be applied to the provided annotation list.
     */
    private final ElementMatcher<? super AnnotationList> matcher;

    /**
     * Creates a new matcher for the inherited annotations of a type description.
     *
     * @param matcher The matcher to be applied to the provided annotation list.
     */
    public InheritedAnnotationMatcher(ElementMatcher<? super AnnotationList> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matcher.matches(target.getInheritedAnnotations());
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "inheritsAnnotations(" + matcher + ")";
    }
}
