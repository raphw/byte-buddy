package net.bytebuddy.description;

import net.bytebuddy.description.type.TypeDefinition;

/**
 * This interface represents all elements that can be declared within a type, i.e. other types and type members.
 */
public interface DeclaredByType {

    /**
     * Returns the declaring type of this instance.
     *
     * @return The declaring type or {@code null} if no such type exists.
     */
    TypeDefinition getDeclaringType();
}
