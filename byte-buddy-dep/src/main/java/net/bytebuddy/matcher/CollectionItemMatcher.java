package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

/**
 * A list item matcher matches any element of a collection to a given matcher and assures that at least one
 * element matches the supplied iterable condition.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class CollectionItemMatcher<T> extends ElementMatcher.Junction.AbstractBase<Iterable<? extends T>> {

    /**
     * The element matcher to apply to each element of a collection.
     */
    private final ElementMatcher<? super T> matcher;

    /**
     * Creates a new matcher that applies another matcher to each element of a matched iterable collection.
     *
     * @param matcher The element matcher to apply to each element of a iterable collection.
     */
    public CollectionItemMatcher(ElementMatcher<? super T> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(Iterable<? extends T> target) {
        for (T value : target) {
            if (matcher.matches(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "whereOne(" + matcher + ")";
    }
}
