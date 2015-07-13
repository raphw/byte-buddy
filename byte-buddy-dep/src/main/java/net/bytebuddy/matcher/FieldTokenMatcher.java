package net.bytebuddy.matcher;

import net.bytebuddy.description.field.FieldDescription;

/**
 * An element matcher that matches the matched field's token.
 *
 * @param <T> The type of the matched entity.
 */
public class FieldTokenMatcher<T extends FieldDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * A matcher to be applied to the matched field's token.
     */
    private final ElementMatcher<? super FieldDescription.Token> matcher;

    /**
     * Creates a new field token matcher.
     *
     * @param matcher A field to be applied to the matched field's token.
     */
    public FieldTokenMatcher(ElementMatcher<? super FieldDescription.Token> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.asToken());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((FieldTokenMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "representedBy(" + matcher + ')';
    }
}
