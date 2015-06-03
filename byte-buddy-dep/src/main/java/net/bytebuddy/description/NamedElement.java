package net.bytebuddy.description;

/**
 * Represents a Java element with a name.
 */
public interface NamedElement {

    /**
     * Represents an element without a name in the source code.
     */
    String EMPTY_NAME = "";

    String getSourceCodeName();

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

    interface WithGenericName extends WithRuntimeName {

        String toGenericString();
    }
}
