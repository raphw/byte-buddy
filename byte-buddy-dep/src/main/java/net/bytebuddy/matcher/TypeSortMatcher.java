package net.bytebuddy.matcher;

import net.bytebuddy.description.type.generic.GenericTypeDescription;

public class TypeSortMatcher<T extends GenericTypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super GenericTypeDescription.Sort> matcher;

    public TypeSortMatcher(ElementMatcher<? super GenericTypeDescription.Sort> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getSort());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((TypeSortMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "ofSort(" + matcher + ')';
    }
}
