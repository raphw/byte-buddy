package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

/**
 * An element matcher that matches its argument's return type against a given type matcher.
 *
 * @param <T> The type of the matched entity.
 */
public class MethodReturnTypeMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type matcher to apply to the matched element's return type.
     */
    private final ElementMatcher<? super GenericTypeDescription> typeMatcher;

    /**
     * Creates a new matcher for a matched element's return type.
     *
     * @param typeMatcher The type matcher to apply to the matched element's return type.
     */
    public MethodReturnTypeMatcher(ElementMatcher<? super GenericTypeDescription> typeMatcher) {
        this.typeMatcher = typeMatcher;
    }

    @Override
    public boolean matches(T target) {
        return typeMatcher.matches(target.getReturnType());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && typeMatcher.equals(((MethodReturnTypeMatcher<?>) other).typeMatcher);
    }

    @Override
    public int hashCode() {
        return typeMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "returns(" + typeMatcher + ")";
    }
}
