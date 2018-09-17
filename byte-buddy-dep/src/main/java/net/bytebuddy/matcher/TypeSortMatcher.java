package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDefinition;

/**
 * An element matcher that validates that a given generic type description represents a type of a given name.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class TypeSortMatcher<T extends TypeDefinition> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * An element matcher to be applied to the type's sort.
     */
    private final ElementMatcher<? super TypeDefinition.Sort> matcher;

    /**
     * Creates a new type sort matcher.
     *
     * @param matcher An element matcher to be applied to the type's sort.
     */
    public TypeSortMatcher(ElementMatcher<? super TypeDefinition.Sort> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matcher.matches(target.getSort());
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "ofSort(" + matcher + ')';
    }
}
