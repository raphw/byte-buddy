package net.bytebuddy.matcher;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.type.TypeDefinition;

/**
 * An element matcher that checks if a type description declares fields of a given property.
 *
 * @param <T> The exact type of the annotated element that is matched.
 */
@EqualsAndHashCode(callSuper = false)
public class DeclaringFieldMatcher<T extends TypeDefinition> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The field matcher to apply to the declared fields of the matched type description.
     */
    private final ElementMatcher<? super FieldList<?>> matcher;

    /**
     * Creates a new matcher for a type's declared fields.
     *
     * @param matcher The field matcher to apply to the declared fields of the matched type description.
     */
    public DeclaringFieldMatcher(ElementMatcher<? super FieldList<? extends FieldDescription>> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getDeclaredFields());
    }

    @Override
    public String toString() {
        return "declaresFields(" + matcher + ")";
    }
}
