package net.bytebuddy.description;

/**
 * Represents a Java element with a name.
 */
public interface NamedElement {

    /**
     * Indicates that an element is not named.
     */
    String NO_NAME = null;

    /**
     * Represents an element without a name in the source code.
     */
    String EMPTY_NAME = "";

    /**
     * Returns the name of this element as it is found in the source code. If no such name exists,
     * an empty string is returned.
     *
     * @return The name of this element as given in a Java program's source code.
     */
    String getActualName();

    /**
     * A named element with a name that has a particular meaning to the Java runtime.
     */
    interface WithRuntimeName extends NamedElement {

        /**
         * Returns the internalName of this byte code element.
         *
         * @return The internalName of this byte code element as visible from within a running Java application.
         */
        String getName();

        /**
         * Returns the internal internalName of this byte code element.
         *
         * @return The internal internalName of this byte code element as used within the Java class file format.
         */
        String getInternalName();
    }

    /**
     * Describes a named element where naming the element is optional and an alternative default name is provided if no explicit name is given.
     */
    interface WithOptionalName extends NamedElement {

        /**
         * Returns {@code true} if this element has an explicit name.
         *
         * @return {@code true} if this element has an explicit name.
         */
        boolean isNamed();
    }

    /**
     * A named element with a generic type name.
     */
    interface WithGenericName extends WithRuntimeName {

        /**
         * Returns a generic string of this byte code element.
         *
         * @return A generic string of this byte code element.
         */
        String toGenericString();
    }
}
