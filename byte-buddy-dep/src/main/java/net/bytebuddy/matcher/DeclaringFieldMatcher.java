package net.bytebuddy.matcher;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

/**
 * An element matcher that checks if a type description declares fields of a given property.
 *
 * @param <T> The exact type of the annotated element that is matched.
 */
public class DeclaringFieldMatcher<T extends GenericTypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The field matcher to apply to the declared fields of the matched type description.
     */
    private final ElementMatcher<? super FieldList<?>> fieldMatcher;

    /**
     * Creates a new matcher for a type's declared fields.
     *
     * @param fieldMatcher The field matcher to apply to the declared fields of the matched type description.
     */
    public DeclaringFieldMatcher(ElementMatcher<? super FieldList<? extends FieldDescription>> fieldMatcher) {
        this.fieldMatcher = fieldMatcher;
    }

    @Override
    public boolean matches(T target) {
        return fieldMatcher.matches(target.getDeclaredFields());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && fieldMatcher.equals(((DeclaringFieldMatcher<?>) other).fieldMatcher);
    }

    @Override
    public int hashCode() {
        return fieldMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "declaresFields(" + fieldMatcher + ")";
    }
}
