package net.bytebuddy.matcher;

import lombok.EqualsAndHashCode;

/**
 * An element matcher that matches the {@code null} value.
 *
 * @param <T> The type of the matched entity.
 */
@EqualsAndHashCode(callSuper = false)
public class NullMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    @Override
    public boolean matches(T target) {
        return target == null;
    }

    @Override
    public String toString() {
        return "isNull()";
    }
}
