package net.bytebuddy.utility.privilege;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A privileged action for reading the system class loader.
 */
public enum SystemClassLoaderAction implements PrivilegedAction<ClassLoader> {

    /**
     * The singleton instance.
     */
    INSTANCE;

    /**
     * Returns the system class loader.
     *
     * @param accessControlContext The access control context to use.
     * @return The value of {@link ClassLoader#getSystemClassLoader()}.
     */
    public static ClassLoader apply(AccessControlContext accessControlContext) {
        return AccessController.doPrivileged(INSTANCE, accessControlContext);
    }

    @Override
    public ClassLoader run() {
        return ClassLoader.getSystemClassLoader();
    }

    @Override
    public String toString() {
        return "SystemClassLoaderAction." + name();
    }
}
