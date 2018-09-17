package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

/**
 * An element matcher that checks an object's equality to another object.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class EqualityMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The object that is checked to be equal to the matched value.
     */
    private final Object value;

    /**
     * Creates an element matcher that tests for equality.
     *
     * @param value The object that is checked to be equal to the matched value.
     */
    public EqualityMatcher(Object value) {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return value.equals(target);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "is(" + value + ")";
    }
}
