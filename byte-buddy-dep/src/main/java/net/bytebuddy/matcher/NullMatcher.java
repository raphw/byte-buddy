package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

/**
 * An element matcher that matches the {@code null} value.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class NullMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return target == null;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "isNull()";
    }
}
