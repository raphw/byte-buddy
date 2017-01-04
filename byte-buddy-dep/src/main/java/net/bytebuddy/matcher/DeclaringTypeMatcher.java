package net.bytebuddy.matcher;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.DeclaredByType;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches the declaring type of another element, only if this element is actually declared
 * in a type.
 *
 * @param <T> The exact type of the element being matched.
 */
@EqualsAndHashCode(callSuper = false)
public class DeclaringTypeMatcher<T extends DeclaredByType> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type matcher to be applied if the target element is declared in a type.
     */
    private final ElementMatcher<? super TypeDescription.Generic> matcher;

    /**
     * Creates a new matcher for the declaring type of an element.
     *
     * @param matcher The type matcher to be applied if the target element is declared in a type.
     */
    public DeclaringTypeMatcher(ElementMatcher<? super TypeDescription.Generic> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        TypeDefinition declaringType = target.getDeclaringType();
        return declaringType != null && matcher.matches(declaringType.asGenericType());
    }

    @Override
    public String toString() {
        return "declaredBy(" + matcher + ")";
    }
}
