package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.type.TypeDescription;

public class SubTypeMatcher<T extends TypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final TypeDescription typeDescription;

    public SubTypeMatcher(TypeDescription typeDescription) {
        this.typeDescription = typeDescription;
    }

    @Override
    public boolean matches(T target) {
        return target.isAssignableTo(typeDescription);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && typeDescription.equals(((SubTypeMatcher) other).typeDescription);
    }

    @Override
    public int hashCode() {
        return typeDescription.hashCode();
    }

    @Override
    public String toString() {
        return "isSubTypeOf(" + typeDescription + ')';
    }
}
