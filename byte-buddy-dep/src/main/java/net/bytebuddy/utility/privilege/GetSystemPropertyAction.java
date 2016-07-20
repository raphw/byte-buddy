package net.bytebuddy.utility.privilege;

import java.security.PrivilegedAction;

/**
 * An action for reading a system property as a privileged action.
 */
public class GetSystemPropertyAction implements PrivilegedAction<String> {

    /**
     * The property key.
     */
    private final String key;

    /**
     * Creates a new action for reading a system property.
     *
     * @param key The property key.
     */
    public GetSystemPropertyAction(String key) {
        this.key = key;
    }

    @Override
    public String run() {
        return System.getProperty(key);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        GetSystemPropertyAction that = (GetSystemPropertyAction) object;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "GetSystemPropertyAction{" +
                "key='" + key + '\'' +
                '}';
    }
}
