package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

/**
 * An element matcher that matches a class loader for being a parent of the given class loader.
 *
 * @param <T> The exact type of the class loader that is matched.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ClassLoaderParentMatcher<T extends ClassLoader> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The class loader that is matched for being a child of the matched class loader.
     */
    private final ClassLoader classLoader;

    /**
     * Creates a class loader parent element matcher.
     *
     * @param classLoader The class loader that is matched for being a child of the matched class loader.
     */
    public ClassLoaderParentMatcher(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public boolean matches(T target) {
        ClassLoader current = classLoader;
        while (current != null) {
            if (current == target) {
                return true;
            }
            current = current.getParent();
        }
        return target == null;
    }

    @Override
    public String toString() {
        return "isParentOf(" + classLoader + ')';
    }
}
