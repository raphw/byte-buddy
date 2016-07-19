package net.bytebuddy.matcher;

import net.bytebuddy.utility.privilege.ParentClassLoaderAction;

import java.security.AccessControlContext;

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
     * The access control context to use.
     */
    private final AccessControlContext accessControlContext;

    /**
     * Creates a new class loader hierarchy matcher.
     *
     * @param matcher              The matcher to apply on each class loader in the hierarchy.
     * @param accessControlContext The access control context to use.
     */
    public ClassLoaderHierarchyMatcher(ElementMatcher<? super ClassLoader> matcher, AccessControlContext accessControlContext) {
        this.matcher = matcher;
        this.accessControlContext = accessControlContext;
    }

    @Override
    public boolean matches(T target) {
        ClassLoader current = target;
        while (current != null) {
            if (matcher.matches(current)) {
                return true;
            }
            current = ParentClassLoaderAction.apply(current, accessControlContext);
        }
        return matcher.matches(null);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((ClassLoaderHierarchyMatcher<?>) other).matcher)
                && accessControlContext.equals(((ClassLoaderHierarchyMatcher<?>) other).accessControlContext);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode() + 31 * accessControlContext.hashCode();
    }

    @Override
    public String toString() {
        return "hasChild(" + matcher + ')';
    }
}
