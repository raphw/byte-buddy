package net.bytebuddy.matcher;

import net.bytebuddy.description.NamedElement;

/**
 * An element matcher that matches a named element only if is explicitly named.
 *
 * @param <T> The type of the matched entity.
 */
public class IsNamedMatcher<T extends NamedElement.WithOptionalName> extends ElementMatcher.Junction.AbstractBase<T> {

    @Override
    public boolean matches(T target) {
        return target.isNamed();
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
        return "isNamed()";
    }
}
