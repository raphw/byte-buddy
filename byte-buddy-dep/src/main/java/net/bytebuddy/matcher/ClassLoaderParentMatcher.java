package net.bytebuddy.matcher;

import net.bytebuddy.utility.privilege.ParentClassLoaderAction;

import java.security.AccessControlContext;

/**
 * An element matcher that matches a class loader for being a parent of the given class loader.
 *
 * @param <T> The exact type of the class loader that is matched.
 */
public class ClassLoaderParentMatcher<T extends ClassLoader> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The class loader that is matched for being a child of the matched class loader.
     */
    private final ClassLoader classLoader;

    /**
     * The access control context to use.
     */
    private final AccessControlContext accessControlContext;

    /**
     * Creates a class loader parent element matcher.
     *
     * @param classLoader The class loader that is matched for being a child of the matched class loader.
     * @param accessControlContext The access control context to use.
     */
    public ClassLoaderParentMatcher(ClassLoader classLoader, AccessControlContext accessControlContext) {
        this.classLoader = classLoader;
        this.accessControlContext = accessControlContext;
    }

    @Override
    public boolean matches(T target) {
        ClassLoader current = classLoader;
        while (current != null) {
            if (current == target) {
                return true;
            }
            current = ParentClassLoaderAction.apply(current, accessControlContext);
        }
        return target == null;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && classLoader.equals(((ClassLoaderParentMatcher) other).classLoader)
                && accessControlContext.equals(((ClassLoaderParentMatcher) other).accessControlContext);
    }

    @Override
    public int hashCode() {
        return classLoader.hashCode();
    }

    @Override
    public String toString() {
        return "isParentOf(" + classLoader + ')';
    }
}
