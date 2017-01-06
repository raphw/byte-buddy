package net.bytebuddy.matcher;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;

/**
 * An element matcher that checks if a type description declares methods of a given property.
 *
 * @param <T> The exact type of the annotated element that is matched.
 */
@EqualsAndHashCode(callSuper = false)
public class DeclaringMethodMatcher<T extends TypeDefinition> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The field matcher to apply to the declared fields of the matched type description.
     */
    private final ElementMatcher<? super MethodList<?>> matcher;

    /**
     * Creates a new matcher for a type's declared methods.
     *
     * @param matcher The method matcher to apply to the declared methods of the matched type description.
     */
    public DeclaringMethodMatcher(ElementMatcher<? super MethodList<? extends MethodDescription>> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getDeclaredMethods());
    }

    @Override
    public String toString() {
        return "declaresMethods(" + matcher + ")";
    }
}
