package net.bytebuddy.matcher;

import lombok.EqualsAndHashCode;

import java.util.Iterator;

/**
 * A matcher that matches a given element of a collection. If no such element is contained by the matched iterable, the matcher
 * returns {@code false}.
 *
 * @param <T> The type of the elements contained by the collection.
 */
@EqualsAndHashCode(callSuper = false)
public class CollectionElementMatcher<T> extends ElementMatcher.Junction.AbstractBase<Iterable<? extends T>> {

    /**
     * The index of the matched element.
     */
    private final int index;

    /**
     * The matcher for the given element, if it exists.
     */
    private final ElementMatcher<? super T> matcher;

    /**
     * Creates a new matcher for an element in a collection.
     *
     * @param index          The index of the matched element.
     * @param matcher The matcher for the given element, if it exists.
     */
    public CollectionElementMatcher(int index, ElementMatcher<? super T> matcher) {
        this.index = index;
        this.matcher = matcher;
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
        return iterator.hasNext() && matcher.matches(iterator.next());
    }

    @Override
    public String toString() {
        return "with(" + index + " matches " + matcher + ")";
    }
}
