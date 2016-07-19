package net.bytebuddy.utility.privilege;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A privileged action for reading a class loader's parent.
 */
public class ParentClassLoaderAction implements PrivilegedAction<ClassLoader> {

    /**
     * The class loader for which to read the parent.
     */
    private final ClassLoader classLoader;

    /**
     * Creates a new parent class loader action.
     *
     * @param classLoader The class loader for which to read the parent. Must not be the bootstrap class loader which is {@code null}.
     */
    public ParentClassLoaderAction(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Reads a type's parent class loader.
     *
     * @param classLoader          The class loader for which to read the parent. Must not be the bootstrap class loader which is {@code null}.
     * @param accessControlContext The access control context to use.
     * @return The class loader's parent class loader.
     */
    public static ClassLoader apply(ClassLoader classLoader, AccessControlContext accessControlContext) {
        return AccessController.doPrivileged(new ParentClassLoaderAction(classLoader), accessControlContext);
    }

    @Override
    public ClassLoader run() {
        return classLoader.getParent();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ParentClassLoaderAction that = (ParentClassLoaderAction) object;
        return classLoader.equals(that.classLoader);
    }

    @Override
    public int hashCode() {
        return classLoader.hashCode();
    }

    @Override
    public String toString() {
        return "ParentClassLoaderAction{" +
                "classLoader=" + classLoader +
                '}';
    }
}
