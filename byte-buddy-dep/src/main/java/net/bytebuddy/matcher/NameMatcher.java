package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.ByteCodeElement;

public class NameMatcher<T extends ByteCodeElement> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<String> nameMatcher;

    public NameMatcher(ElementMatcher<String> nameMatcher) {
        this.nameMatcher = nameMatcher;
    }

    @Override
    public boolean matches(T target) {
        return nameMatcher.matches(target.getName());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && nameMatcher.equals(((NameMatcher) other).nameMatcher);
    }

    @Override
    public int hashCode() {
        return nameMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "name(" + nameMatcher + ")";
    }
}
