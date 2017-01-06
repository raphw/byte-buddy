package net.bytebuddy.matcher;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches the type of an annotation description.
 *
 * @param <T> The exact type of the annotation description that is matched.
 */
@EqualsAndHashCode(callSuper = false)
public class AnnotationTypeMatcher<T extends AnnotationDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type matcher to apply to an annotation's type.
     */
    private final ElementMatcher<? super TypeDescription> matcher;

    /**
     * Creates a new matcher for an annotation description's type.
     *
     * @param matcher The type matcher to apply to an annotation's type.
     */
    public AnnotationTypeMatcher(ElementMatcher<? super TypeDescription> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getAnnotationType());
    }

    @Override
    public String toString() {
        return "ofAnnotationType(" + matcher + ')';
    }
}
