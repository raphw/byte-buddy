package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches an object's type.
 *
 * @param <T> The exact type of the object that is matched.
 */
@HashCodeAndEqualsPlugin.Enhance
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

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return target != null && matcher.matches(TypeDescription.ForLoadedType.of(target.getClass()));
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "ofType(" + matcher + ")";
    }
}
