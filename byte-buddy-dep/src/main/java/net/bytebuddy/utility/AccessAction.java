package net.bytebuddy.utility;

import java.lang.reflect.AccessibleObject;
import java.security.PrivilegedAction;

/**
 * An action for making an {@link AccessibleObject} accessible.
 *
 * @param <T> The type of the accessible object.
 */
public class AccessAction<T extends AccessibleObject> implements PrivilegedAction<T> {

    /**
     * The accessible object.
     */
    private final T accessibleObject;

    /**
     * Creates a new access action.
     *
     * @param accessibleObject The accessible object.
     */
    protected AccessAction(T accessibleObject) {
        this.accessibleObject = accessibleObject;
    }

    /**
     * Creates a new access action that returns the accessible object on execution.
     *
     * @param accessibleObject The accessible object.
     * @param <S>              The type of the accessible object.
     * @return An access action that makes the accessible object accessible and returns it subsequently.
     */
    public static <S extends AccessibleObject> PrivilegedAction<S> of(S accessibleObject) {
        return new AccessAction<S>(accessibleObject);
    }

    @Override
    public T run() {
        accessibleObject.setAccessible(true);
        return accessibleObject;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && accessibleObject.equals(((AccessAction<?>) other).accessibleObject);
    }

    @Override
    public int hashCode() {
        return accessibleObject.hashCode();
    }

    @Override
    public String toString() {
        return "AccessAction{" +
                "accessibleObject=" + accessibleObject +
                '}';
    }
}
