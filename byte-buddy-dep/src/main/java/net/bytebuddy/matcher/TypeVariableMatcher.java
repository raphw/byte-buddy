package net.bytebuddy.matcher;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

public class TypeVariableMatcher<T extends GenericTypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super NamedElement> matcher;

    public TypeVariableMatcher(ElementMatcher<? super NamedElement> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return target.getSort().isTypeVariable() && matcher.matches(target);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((TypeVariableMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "isVariable(" + matcher + ')';
    }
}
