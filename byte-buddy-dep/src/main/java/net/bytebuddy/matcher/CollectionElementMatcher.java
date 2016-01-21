package net.bytebuddy.matcher;

import java.util.Iterator;

/**
 * A matcher that matches a given element of a collection. If no such element is contained by the matched iterable, the matcher
 * returns {@code false}.
 *
 * @param <T> The type of the elements contained by the collection.
 */
public class CollectionElementMatcher<T> extends ElementMatcher.Junction.AbstractBase<Iterable<? extends T>> {

    /**
     * The index of the matched element.
     */
    private final int index;

    /**
     * The matcher for the given element, if it exists.
     */
    private final ElementMatcher<? super T> elementMatcher;

    /**
     * Creates a new matcher for an element in a collection.
     *
     * @param index          The index of the matched element.
     * @param elementMatcher The matcher for the given element, if it exists.
     */
    public CollectionElementMatcher(int index, ElementMatcher<? super T> elementMatcher) {
        this.index = index;
        this.elementMatcher = elementMatcher;
    }

    @Override
    public boolean matches(Iterable<? extends T> target) {
        Iterator<? extends T> iterator = target.iterator();
        for (int index = 0; index < this.index; index++) {
            if (iterator.hasNext()) {
                iterator.next();
            } else {
                return false;
            }
        }
        return iterator.hasNext() && elementMatcher.matches(iterator.next());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && index == ((CollectionElementMatcher<?>) other).index
                && elementMatcher.equals(((CollectionElementMatcher<?>) other).elementMatcher);
    }

    @Override
    public int hashCode() {
        return elementMatcher.hashCode() + 31 * index;
    }

    @Override
    public String toString() {
        return "with(" + index + " matches " + elementMatcher + ")";
    }
}
