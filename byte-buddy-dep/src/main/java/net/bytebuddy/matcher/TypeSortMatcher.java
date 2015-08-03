package net.bytebuddy.matcher;

import net.bytebuddy.description.type.generic.GenericTypeDescription;

/**
 * An element matcher that validates that a given generic type description represents a type of a given name.
 *
 * @param <T> The type of the matched entity.
 */
public class TypeSortMatcher<T extends GenericTypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * An element matcher to be applied to the type's sort.
     */
    private final ElementMatcher<? super GenericTypeDescription.Sort> matcher;

    /**
     * Creates a new type sort matcher.
     *
     * @param matcher An element matcher to be applied to the type's sort.
     */
    public TypeSortMatcher(ElementMatcher<? super GenericTypeDescription.Sort> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getSort());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((TypeSortMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "ofSort(" + matcher + ')';
    }
}
