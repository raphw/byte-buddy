package net.bytebuddy.utility.privilege;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

/**
 * A privileged action for reading a type's protection domain.
 */
public class ProtectionDomainAction implements PrivilegedAction<ProtectionDomain> {

    /**
     * The type for which to read the protection domain.
     */
    private final Class<?> type;

    /**
     * Creates a new action for reading a type's protection domain.
     *
     * @param type The type for which to read the protection domain.
     */
    public ProtectionDomainAction(Class<?> type) {
        this.type = type;
    }

    /**
     * Reads a type's protection domain using a privileged action.
     *
     * @param type                 The type for which to read the protection domain.
     * @param accessControlContext The access control context to use.
     * @return The type's protection domain.
     */
    public static ProtectionDomain apply(Class<?> type, AccessControlContext accessControlContext) {
        return AccessController.doPrivileged(new ProtectionDomainAction(type), accessControlContext);
    }

    @Override
    public ProtectionDomain run() {
        return type.getProtectionDomain();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ProtectionDomainAction that = (ProtectionDomainAction) object;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return "ProtectionDomainAction{" +
                "type=" + type +
                '}';
    }
}
