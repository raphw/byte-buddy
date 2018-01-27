package net.bytebuddy.matcher;

import com.google.auto.value.AutoValue;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches its argument for being another type's subtype.
 *
 * @param <T> The type of the matched entity.
 */
@AutoValue
public class SubTypeMatcher<T extends TypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type to be matched being a super type of the matched type.
     */
    private final TypeDescription typeDescription;

    /**
     * Creates a new matcher for matching its input for being a sub type of the given {@code typeDescription}.
     *
     * @param typeDescription The type to be matched being a super type of the matched type.
     */
    public SubTypeMatcher(TypeDescription typeDescription) {
        this.typeDescription = typeDescription;
    }

    @Override
    public boolean matches(T target) {
        return target.isAssignableTo(typeDescription);
    }

    @Override
    public String toString() {
        return "isSubTypeOf(" + typeDescription + ')';
    }
}

