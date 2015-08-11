package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

/**
 * An element matcher that matches its argument's {@link GenericTypeDescription} raw type against the
 * given matcher for a {@link TypeDescription}. A wildcard is not matched but returns a negative result.
 *
 * @param <T> The type of the matched entity.
 */
public class RawTypeMatcher<T extends GenericTypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the raw type of the matched element.
     */
    private final ElementMatcher<? super TypeDescription> rawTypeMatcher;

    /**
     * Creates a new raw type matcher.
     *
     * @param rawTypeMatcher The matcher to apply to the raw type.
     */
    public RawTypeMatcher(ElementMatcher<? super TypeDescription> rawTypeMatcher) {
        this.rawTypeMatcher = rawTypeMatcher;
    }

    @Override
    public boolean matches(T target) {
        return !target.getSort().isWildcard() && rawTypeMatcher.matches(target.asErasure());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && rawTypeMatcher.equals(((RawTypeMatcher<?>) other).rawTypeMatcher);
    }

    @Override
    public int hashCode() {
        return rawTypeMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "rawType(" + rawTypeMatcher + ")";
    }
}
