package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches an object's type.
 *
 * @param <T> The exact type of the object that is matched.
 */
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
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        InstanceTypeMatcher<?> that = (InstanceTypeMatcher<?>) object;
        return matcher.equals(that.matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "ofType(" + matcher + ")";
    }
}
