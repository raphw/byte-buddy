package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches its argument's return type against a given type matcher.
 *
 * @param <T> The type of the matched entity.
 */
public class MethodReturnTypeMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type matcher to apply to the matched element's return type.
     */
    private final ElementMatcher<? super TypeDescription.Generic> matcher;

    /**
     * Creates a new matcher for a matched element's return type.
     *
     * @param matcher The type matcher to apply to the matched element's return type.
     */
    public MethodReturnTypeMatcher(ElementMatcher<? super TypeDescription.Generic> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getReturnType());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((MethodReturnTypeMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "returns(" + matcher + ")";
    }
}
