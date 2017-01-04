package net.bytebuddy.matcher;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.type.TypeDefinition;

/**
 * An element matcher that validates that a given generic type description represents a type of a given name.
 *
 * @param <T> The type of the matched entity.
 */
@EqualsAndHashCode(callSuper = false)
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

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getSort());
    }

    @Override
    public String toString() {
        return "ofSort(" + matcher + ')';
    }
}
