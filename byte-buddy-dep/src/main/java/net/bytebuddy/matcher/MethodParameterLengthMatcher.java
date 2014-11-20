package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;

public class MethodParameterLengthMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final int length;

    public MethodParameterLengthMatcher(int length) {
        this.length = length;
    }

    @Override
    public boolean matches(T target) {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && length == ((MethodParameterLengthMatcher) other).length;
    }

    @Override
    public int hashCode() {
        return length;
    }

    @Override
    public String toString() {
        return "hasParameters(" + length + ")";
    }
}
