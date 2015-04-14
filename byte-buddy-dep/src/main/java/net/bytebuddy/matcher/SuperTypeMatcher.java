package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches its argument for being another type's super type.
 *
 * @param <T> The type of the matched entity.
 */
public class SuperTypeMatcher<T extends TypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type to be matched being a sub type of the matched type.
     */
    private final TypeDescription typeDescription;

    /**
     * Creates a new matcher for matching its input for being a super type of the given {@code typeDescription}.
     *
     * @param typeDescription The type to be matched being a sub type of the matched type.
     */
    public SuperTypeMatcher(TypeDescription typeDescription) {
        this.typeDescription = typeDescription;
    }

    @Override
    public boolean matches(T target) {
        return target.isAssignableFrom(typeDescription);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && typeDescription.equals(((SuperTypeMatcher) other).typeDescription);
    }

    @Override
    public int hashCode() {
        return typeDescription.hashCode();
    }

    @Override
    public String toString() {
        return "isSuperTypeOf(" + typeDescription + ')';
    }
}
