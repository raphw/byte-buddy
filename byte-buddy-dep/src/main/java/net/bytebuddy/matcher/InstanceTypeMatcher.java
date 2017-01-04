package net.bytebuddy.matcher;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches an object's type.
 *
 * @param <T> The exact type of the object that is matched.
 */
@EqualsAndHashCode(callSuper = false)
public class InstanceTypeMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the object's type.
     */
    private final ElementMatcher<? super TypeDescription> matcher;

    /**
     * Creates a new instance type matcher.
     *
     * @param matcher The matcher to apply to the object's type.
     */
    public InstanceTypeMatcher(ElementMatcher<? super TypeDescription> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return target != null && matcher.matches(new TypeDescription.ForLoadedType(target.getClass()));
    }

    @Override
    public String toString() {
        return "ofType(" + matcher + ")";
    }
}
