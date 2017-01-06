package net.bytebuddy.utility.privilege;

import lombok.EqualsAndHashCode;

import java.lang.reflect.AccessibleObject;
import java.security.PrivilegedAction;

/**
 * An action for making an {@link AccessibleObject} accessible.
 *
 * @param <T> The type of the accessible object.
 */
@EqualsAndHashCode
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
}
