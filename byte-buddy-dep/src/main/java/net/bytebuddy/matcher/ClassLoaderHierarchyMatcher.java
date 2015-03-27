package net.bytebuddy.matcher;

/**
 * An element matcher that matches all {@link java.lang.ClassLoader}s in the matched class loaders hierarchy
 * against a given matcher.
 *
 * @param <T> The exact type of the class loader that is matched.
 */
public class ClassLoaderHierarchyMatcher<T extends ClassLoader> implements ElementMatcher<T> {

    /**
     * The matcher to apply on each class loader in the hierarchy.
     */
    private final ElementMatcher<? super ClassLoader> classLoaderMatcher;

    /**
     * Creates a new class loader hierarchy matcher.
     *
     * @param classLoaderMatcher The matcher to apply on each class loader in the hierarchy.
     */
    public ClassLoaderHierarchyMatcher(ElementMatcher<? super ClassLoader> classLoaderMatcher) {
        this.classLoaderMatcher = classLoaderMatcher;
    }

    @Override
    public boolean matches(T target) {
        ClassLoader current = target;
        while (current != null) {
            if (classLoaderMatcher.matches(current)) {
                return true;
            }
            current = current.getParent();
        }
        return classLoaderMatcher.matches(null);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && classLoaderMatcher.equals(((ClassLoaderHierarchyMatcher<?>) other).classLoaderMatcher);
    }

    @Override
    public int hashCode() {
        return classLoaderMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "hasChild(" + classLoaderMatcher + ')';
    }
}
