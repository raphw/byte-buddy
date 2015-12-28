package net.bytebuddy.matcher;

import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches a method's parameter's type.
 *
 * @param <T> The type of the matched entity.
 */
public class MethodParameterTypeMatcher<T extends ParameterDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the type of the parameter.
     */
    private final ElementMatcher<? super TypeDescription.Generic> matcher;

    /**
     * Creates a new matcher for a method's parameter's type.
     *
     * @param matcher The matcher to apply to the type of the parameter.
     */
    public MethodParameterTypeMatcher(ElementMatcher<? super TypeDescription.Generic> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getType());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((MethodParameterTypeMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "hasType(" + matcher + ")";
    }
}
