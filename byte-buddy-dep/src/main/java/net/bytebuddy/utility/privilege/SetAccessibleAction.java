package net.bytebuddy.utility.privilege;

import java.lang.reflect.AccessibleObject;
import java.security.PrivilegedAction;

/**
 * An action for making an {@link AccessibleObject} accessible.
 *
 * @param <T> The type of the accessible object.
 */
public class SetAccessibleAction<T extends AccessibleObject> implements PrivilegedAction<T> {

    /**
     * The accessible object.
     */
    private final T accessibleObject;

    /**
     * Creates a new access action.
     *
     * @param accessibleObject The accessible object.
     */
    public SetAccessibleAction(T accessibleObject) {
        this.accessibleObject = accessibleObject;
    }

    @Override
    public T run() {
        accessibleObject.setAccessible(true);
        return accessibleObject;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && accessibleObject.equals(((SetAccessibleAction<?>) other).accessibleObject);
    }

    @Override
    public int hashCode() {
        return accessibleObject.hashCode();
    }

    @Override
    public String toString() {
        return "SetAccessibleAction{" +
                "accessibleObject=" + accessibleObject +
                '}';
    }
}
