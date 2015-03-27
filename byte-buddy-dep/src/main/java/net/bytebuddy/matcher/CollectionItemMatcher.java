package net.bytebuddy.matcher;

/**
 * A list item matcher matches any element of a collection to a given matcher and assures that at least one
 * element matches the supplied iterable condition.
 *
 * @param <T> The type of the matched entity.
 */
public class CollectionItemMatcher<T> extends ElementMatcher.Junction.AbstractBase<Iterable<? extends T>> {

    /**
     * The element matcher to apply to each element of a collection.
     */
    private final ElementMatcher<? super T> elementMatcher;

    /**
     * Creates a new matcher that applies another matcher to each element of a matched iterable collection.
     *
     * @param elementMatcher The element matcher to apply to each element of a iterable collection.
     */
    public CollectionItemMatcher(ElementMatcher<? super T> elementMatcher) {
        this.elementMatcher = elementMatcher;
    }

    @Override
    public boolean matches(Iterable<? extends T> target) {
        for (T value : target) {
            if (elementMatcher.matches(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && elementMatcher.equals(((CollectionItemMatcher<?>) other).elementMatcher);
    }

    @Override
    public int hashCode() {
        return elementMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "whereOne(" + elementMatcher + ")";
    }
}
