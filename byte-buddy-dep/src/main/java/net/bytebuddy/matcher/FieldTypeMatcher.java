package net.bytebuddy.matcher;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches a field's type.
 *
 * @param <T> The type of the matched entity.
 */
public class FieldTypeMatcher<T extends FieldDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type matcher to apply to the field's type.
     */
    private final ElementMatcher<? super TypeDescription.Generic> matcher;

    /**
     * Creates a new matcher for a matched field's type.
     *
     * @param matcher The type matcher to apply to the matched field's type.
     */
    public FieldTypeMatcher(ElementMatcher<? super TypeDescription.Generic> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getType());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((FieldTypeMatcher<?>) other).matcher);
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
