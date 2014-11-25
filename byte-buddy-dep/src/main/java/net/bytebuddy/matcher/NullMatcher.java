package net.bytebuddy.matcher;

/**
 * An element matcher that matches the {@code null} value.
 *
 * @param <T> The type of the matched entity.
 */
public class NullMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    @Override
    public boolean matches(T target) {
        return target == null;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass());
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "isNull()";
    }
}
