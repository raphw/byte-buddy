package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.type.DeclaredInType;
import net.bytebuddy.instrumentation.type.TypeDescription;

public class DeclaringTypeMatcher<T extends DeclaredInType> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super TypeDescription> typeMatcher;

    public DeclaringTypeMatcher(ElementMatcher<? super TypeDescription> typeMatcher) {
        this.typeMatcher = typeMatcher;
    }

    @Override
    public boolean matches(T target) {
        return typeMatcher.matches(target.getDeclaringType());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && typeMatcher.equals(((DeclaringTypeMatcher) other).typeMatcher);
    }

    @Override
    public int hashCode() {
        return typeMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "declaringType(" + typeMatcher + ")";
    }
}
