package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches its argument's {@link TypeDescription.Generic} raw type against the
 * given matcher for a {@link TypeDescription}. A wildcard is not matched but returns a negative result.
 *
 * @param <T> The type of the matched entity.
 */
public class RawTypeMatcher<T extends TypeDefinition> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the raw type of the matched element.
     */
    private final ElementMatcher<? super TypeDescription> matcher;

    /**
     * Creates a new raw type matcher.
     *
     * @param matcher The matcher to apply to the raw type.
     */
    public RawTypeMatcher(ElementMatcher<? super TypeDescription> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return !target.getSort().isWildcard() && matcher.matches(target.asErasure());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((RawTypeMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "rawType(" + matcher + ")";
    }
}
