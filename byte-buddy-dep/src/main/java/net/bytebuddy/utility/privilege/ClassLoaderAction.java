package net.bytebuddy.utility.privilege;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A privileged action for reading a type's class loader.
 */
public class ClassLoaderAction implements PrivilegedAction<ClassLoader> {

    /**
     * The type for which to read the class loader.
     */
    private final Class<?> type;

    /**
     * Creates a class loader action.
     *
     * @param type The type for which to read the class loader.
     */
    public ClassLoaderAction(Class<?> type) {
        this.type = type;
    }

    /**
     * Reads a type's class loader.
     *
     * @param type                 The type for which to read the class loader.
     * @param accessControlContext The access control context to use.
     * @return The type's class loader.
     */
    public static ClassLoader apply(Class<?> type, AccessControlContext accessControlContext) {
        return AccessController.doPrivileged(new ClassLoaderAction(type), accessControlContext);
    }

    @Override
    public ClassLoader run() {
        return type.getClassLoader();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ClassLoaderAction that = (ClassLoaderAction) object;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return "ClassLoaderAction{" +
                "type=" + type +
                '}';
    }
}
