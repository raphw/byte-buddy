package net.bytebuddy.matcher;

/**
 * An element matcher that matches all {@link java.lang.ClassLoader}s in the matched class loaders hierarchy
 * against a given matcher.
 *
 * @param <T> The exact type of the class loader that is matched.
 */
public class ClassLoaderHierarchyMatcher<T extends ClassLoader> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply on each class loader in the hierarchy.
     */
    private final ElementMatcher<? super ClassLoader> matcher;

    /**
     * Creates a new class loader hierarchy matcher.
     *
     * @param matcher The matcher to apply on each class loader in the hierarchy.
     */
    public ClassLoaderHierarchyMatcher(ElementMatcher<? super ClassLoader> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        ClassLoader current = target;
        while (current != null) {
            if (matcher.matches(current)) {
                return true;
            }
            current = current.getParent();
        }
        return matcher.matches(null);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((ClassLoaderHierarchyMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "hasChild(" + matcher + ')';
    }
}
