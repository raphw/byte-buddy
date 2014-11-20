package net.bytebuddy.matcher;

public class EqualityMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    private final T value;

    public EqualityMatcher(T value) {
        this.value = value;
    }

    @Override
    public boolean matches(T target) {
        return target.equals(value);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && value.equals(((EqualityMatcher) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "is(" + value + ")";
    }
}
