package net.bytebuddy.matcher;

import net.bytebuddy.description.type.generic.GenericTypeDescription;

public class TypeSymbolMatcher<T extends GenericTypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super String> matcher;

    public TypeSymbolMatcher(ElementMatcher<? super String> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return target.getSort().isTypeVariable() && matcher.matches(target.getSymbol());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((TypeSymbolMatcher<?>) other).matcher);
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
