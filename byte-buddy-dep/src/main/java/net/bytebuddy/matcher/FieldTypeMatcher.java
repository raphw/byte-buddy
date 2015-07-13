package net.bytebuddy.matcher;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

/**
 * An element matcher that matches a field's type.
 *
 * @param <T> The type of the matched entity.
 */
public class FieldTypeMatcher<T extends FieldDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type matcher to apply to the field's type.
     */
    private final ElementMatcher<? super GenericTypeDescription> typeMatcher;

    /**
     * Creates a new matcher for a matched field's type.
     *
     * @param typeMatcher The type matcher to apply to the matched field's type.
     */
    public FieldTypeMatcher(ElementMatcher<? super GenericTypeDescription> typeMatcher) {
        this.typeMatcher = typeMatcher;
    }

    @Override
    public boolean matches(T target) {
        return typeMatcher.matches(target.getType());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && typeMatcher.equals(((FieldTypeMatcher<?>) other).typeMatcher);
    }

    @Override
    public int hashCode() {
        return typeMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "ofType(" + typeMatcher + ")";
    }
}
