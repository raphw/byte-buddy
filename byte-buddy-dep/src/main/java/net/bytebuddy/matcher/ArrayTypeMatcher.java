package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDefinition;

/**
 * Matches an enumeration type.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ArrayTypeMatcher<T extends TypeDefinition> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return target.isArray();
    }

    @Override
    public String toString() {
        return "isArray()";
    }
}
